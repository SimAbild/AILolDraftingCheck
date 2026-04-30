package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CoachService {

    private static final Logger logger = LoggerFactory.getLogger(CoachService.class);
    private static final int FALLBACK_SCORE = 50;

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends drafting coach." +
            " You receive the user's role, chosen champion, their 4 ally picks, and the 5 enemy picks." +

            " RULE 1 — OFF-META DETECTION:" +
            " If the chosen champion is unconventional or off-meta for the given role" +
            " (e.g. Milio mid, Yuumi jungle, Nasus support)," +
            " the FIRST negative MUST explicitly state this:" +
            " name the role, name the champion, and explain in one concrete sentence why it does not work there." +

            " RULE 2 — ALTERNATIVES MUST COMPARE, NOT DESCRIBE:" +
            " Each alternative must be compared DIRECTLY to the user's chosen champion." +
            " Do NOT write generic descriptions of the alternative." +
            " Write what makes it concretely different and better in this specific draft." +
            " This is especially important when alternatives are similar champions" +
            " (e.g. Lulu vs Milio): name the exact mechanical or statistical difference." +

            " Reply with STRICT JSON only, no markdown, in this exact shape:" +
            " {\"positives\":[\"sentence 1\",\"sentence 2\",\"sentence 3\"]," +
            " \"negatives\":[\"sentence 1\",\"sentence 2\",\"sentence 3\"]," +
            " \"alternatives\":[" +
            " {\"champion\":\"Name\"," +
            " \"reason\":\"One sentence that directly compares this champion to [user's champion] and explains why it fits this draft better\"," +
            " \"strengths\":[" +
            " \"Unlike [user's champion], [alternative] can [specific mechanical difference vs this enemy or ally]\"," +
            " \"Counters [specific enemy champion] better than [user's champion] because [concrete ability or stat]\"," +
            " \"[One concrete edge in this draft, e.g. higher peel, more engage, better wave clear]\"" +
            " ]}," +
            " {\"champion\":\"Name\",\"reason\":\"...\",\"strengths\":[\"...\",\"...\",\"...\"]}," +
            " {\"champion\":\"Name\",\"reason\":\"...\",\"strengths\":[\"...\",\"...\",\"...\"]}]}" +
            " Each strengths array must contain exactly 3 entries." +
            " Name specific champions, abilities, and mechanics — never write vague generalities." +
            " Alternatives must be real League of Legends champions that fit the user's role.";

    private static final String SCORING_SYSTEM_MESSAGE =
            "You are a League of Legends scoring system." +
            " Based on the provided OP.GG statistics, calculate a score from 0-100." +
            " Use these weights: Counterpick (60%), Synergy with ally team (20%), Counter against enemy team (20%)." +
            " Reply with STRICT JSON only: {\"score\": <number>}";

    private final OpenAiService openAiService;
    private final OpggService opggService;

    public CoachService(OpenAiService openAiService, OpggService opggService) {
        this.openAiService = openAiService;
        this.opggService = opggService;
    }

    // Fuld coach-analyse: feedback + winrate + score.
    // Bruges i både solo quiz og PvP.
    public CoachResponse analyzeChampionPick(CoachRequest coachRequest) {
        String userPrompt = buildUserPrompt(coachRequest);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, userPrompt);

        try {
            CoachResponse response = openAiService.parseJsonReply(aiReply, CoachResponse.class);
            enrichWithMatchupWinrate(response, coachRequest);
            enrichAlternativesWithWinrate(response, coachRequest);
            enrichWithScore(response, coachRequest);
            return response;
        } catch (Exception e) {
            logger.error("Coach-analyse fejlede: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Coach-analyse fejlede: " + e.getMessage());
        }
    }

    // ── Winrate-berigelse ───────────────────────────────────────────────

    // Henter brugerens champion winrate mod enemy laner via OP.GG matchup-data.
    private void enrichWithMatchupWinrate(CoachResponse response, CoachRequest coachRequest) {
        if (coachRequest.getEnemyTeam() == null) return;

        String enemyLanerName = findEnemyLaner(coachRequest);
        if (enemyLanerName == null) return;

        double winrate = getMatchupWinrate(coachRequest.getUserChampion(), enemyLanerName, coachRequest.getUserRole());
        if (winrate > 0) {
            response.setMatchupWinrate(winrate);
            logger.info("Matchup winrate {} vs {}: {}%", coachRequest.getUserChampion(), enemyLanerName, winrate);
        }
    }

    // Beriger hvert alternativ med matchup-winrate mod enemy laner.
    private void enrichAlternativesWithWinrate(CoachResponse response, CoachRequest coachRequest) {
        if (response.getAlternatives() == null || coachRequest.getEnemyTeam() == null) return;

        String enemyLanerName = findEnemyLaner(coachRequest);
        if (enemyLanerName == null) return;

        for (CoachResponse.Alternative alt : response.getAlternatives()) {
            double winrate = getMatchupWinrate(alt.getName(), enemyLanerName, coachRequest.getUserRole());
            if (winrate > 0) alt.setWinrate(winrate);
        }
    }

    // Henter head-to-head winrate fra OP.GG's matchup-endpoint.
    // Fallback: forsøger at finde winrate i champion-analyse (counter-data).
    private double getMatchupWinrate(String championName, String enemyName, String role) {
        // Forsøg 1: Direkte matchup-guide
        double wr = extractWinrateFromMatchup(championName, enemyName, role);
        if (wr > 0) return wr;

        // Forsøg 2: Hent fra champion-analyse (counter-data)
        logger.info("Matchup-guide gav ingen winrate — forsøger champion-analyse fallback for {} vs {}",
                championName, enemyName);
        return extractWinrateFromAnalysis(championName, enemyName, role);
    }

    // Forsøg 1: Brug lol_get_lane_matchup_guide til head-to-head data.
    private double extractWinrateFromMatchup(String championName, String enemyName, String role) {
        try {
            String matchupText = opggService.getMatchupData(championName, enemyName, role);
            logger.info("OP.GG matchup data for {} vs {} (laengde: {})",
                    championName, enemyName, matchupText != null ? matchupText.length() : 0);
            if (matchupText != null && matchupText.length() > 10) {
                logger.info("OP.GG matchup uddrag: {}",
                        matchupText.substring(0, Math.min(matchupText.length(), 400)));
            }

            if (matchupText != null && !matchupText.isEmpty()) {
                return askOpenAiForWinrate(matchupText, championName, enemyName);
            }
        } catch (Exception e) {
            logger.warn("Matchup-opslag fejlede for {} vs {}: {}", championName, enemyName, e.getMessage());
        }
        return -1;
    }

    // Forsøg 2: Brug lol_get_champion_analysis counter-data som fallback.
    private double extractWinrateFromAnalysis(String championName, String enemyName, String role) {
        try {
            String analysisText = opggService.getChampionAnalysis(championName, role);
            if (analysisText != null && !analysisText.isEmpty()) {
                String systemMsg = "Find the win rate of " + championName + " against " + enemyName +
                        " in the counter data below. Look for " + enemyName + " in counterChampions" +
                        " and extract the win rate. Convert any decimal (e.g. 0.52) to percentage (52.0)." +
                        " Reply with STRICT JSON only: {\"winrate\": <number as percentage>}" +
                        " If " + enemyName + " is not found in the data, reply: {\"winrate\": -1}";
                String truncated = truncate(analysisText, 2000);
                String reply = openAiService.chat(systemMsg, truncated);
                logger.info("OpenAI analyse-fallback winrate-svar for {} vs {}: {}", championName, enemyName, reply);
                return openAiService.parseJsonDouble(reply, "winrate");
            }
        } catch (Exception e) {
            logger.warn("Analyse-fallback fejlede for {} vs {}: {}", championName, enemyName, e.getMessage());
        }
        return -1;
    }

    // Beder OpenAI om at finde winrate i rå OP.GG data (matchup eller analyse).
    private double askOpenAiForWinrate(String opggData, String championName, String enemyName) {
        try {
            String systemMsg = "Extract the win rate of " + championName + " versus " + enemyName +
                    " from the following game data." +
                    " The win rate could be in various formats: a decimal like 0.5234, a percentage like 52.34%," +
                    " or a field named win_rate, winRate, or similar." +
                    " Convert the result to a percentage number (e.g. 52.3)." +
                    " Reply with STRICT JSON only: {\"winrate\": <number as percentage>}" +
                    " If no win rate is found, reply: {\"winrate\": -1}";
            String truncated = truncate(opggData, 2000);
            String reply = openAiService.chat(systemMsg, truncated);
            logger.info("OpenAI winrate-svar for {} vs {}: {}", championName, enemyName, reply);
            double wr = openAiService.parseJsonDouble(reply, "winrate");
            logger.info("Parsed winrate for {} vs {}: {}", championName, enemyName, wr);
            return wr;
        } catch (Exception e) {
            logger.warn("OpenAI winrate-extraction fejlede for {} vs {}: {}", championName, enemyName, e.getMessage());
            return -1;
        }
    }

    // ── Score-beregning ─────────────────────────────────────────────────

    // Beregner en score 0-100 baseret på OP.GG-data og OpenAI.
    // Bruges i både solo quiz og PvP.
    private void enrichWithScore(CoachResponse response, CoachRequest coachRequest) {
        if (coachRequest.getUserChampion() == null) return;

        try {
            String analysisData = opggService.getChampionAnalysis(
                    coachRequest.getUserChampion(), coachRequest.getUserRole());
            String metaData = opggService.getChampionMetaData(
                    coachRequest.getUserChampion(), coachRequest.getUserRole());

            String enemyLaner = findEnemyLaner(coachRequest);
            String allyNames = formatTeamForPrompt(coachRequest.getAllyTeam());
            String enemyNames = formatTeamForPrompt(coachRequest.getEnemyTeam());

            String userMessage = "Champion: " + coachRequest.getUserChampion() +
                    " (" + coachRequest.getUserRole() + ")\n" +
                    "Enemy laner: " + (enemyLaner != null ? enemyLaner : "Unknown") + "\n" +
                    "Ally team: " + allyNames + "\n" +
                    "Enemy team: " + enemyNames + "\n" +
                    "OP.GG analysis data:\n" + truncate(analysisData, 500) + "\n" +
                    "OP.GG meta data:\n" + truncate(metaData, 500);

            String reply = openAiService.chat(SCORING_SYSTEM_MESSAGE, userMessage);
            int score = (int) openAiService.parseJsonDouble(reply, "score");
            response.setScore(Math.min(100, Math.max(0, score)));
            logger.info("Score for {} ({}): {}", coachRequest.getUserChampion(), coachRequest.getUserRole(), score);
        } catch (Exception e) {
            logger.warn("Kunne ikke beregne score: {}", e.getMessage());
            response.setScore(FALLBACK_SCORE);
        }
    }

    // ── Hjælpemetoder ───────────────────────────────────────────────────

    private String findEnemyLaner(CoachRequest coachRequest) {
        if (coachRequest.getEnemyTeam() == null) return null;
        return coachRequest.getEnemyTeam().stream()
                .filter(pick -> pick.getRole().equalsIgnoreCase(coachRequest.getUserRole()))
                .map(DraftPick::getName)
                .findFirst()
                .orElse(null);
    }

    private String buildUserPrompt(CoachRequest coachRequest) {
        return "user_role: " + coachRequest.getUserRole() +
               "\nuser_champion: " + coachRequest.getUserChampion() +
               "\nally_team (4): " + formatTeamForPrompt(coachRequest.getAllyTeam()) +
               "\nenemy_team (5): " + formatTeamForPrompt(coachRequest.getEnemyTeam());
    }

    private String formatTeamForPrompt(List<DraftPick> team) {
        if (team == null || team.isEmpty()) return "(none)";
        return team.stream()
                .map(pick -> pick.getRole() + ":" + pick.getName())
                .collect(Collectors.joining(", "));
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) return "unavailable";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
