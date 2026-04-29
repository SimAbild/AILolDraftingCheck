package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.dtos.WinrateResponse;
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
    private static final int MAX_ALTERNATIVES = 3;

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

    private final OpenAiService openAiService;
    private final OpggService opggService;

    public CoachService(OpenAiService openAiService, OpggService opggService) {
        this.openAiService = openAiService;
        this.opggService = opggService;
    }

    public CoachResponse analyzeChampionPick(CoachRequest coachRequest) {
        String userPrompt = buildUserPrompt(coachRequest);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, userPrompt);

        try {
            CoachResponse response = openAiService.parseJsonReply(aiReply, CoachResponse.class);
            enrichAlternativesWithWinrate(response, coachRequest);
            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    // Beriger hvert alternativ med winrate-data fra OP.GG.
    // Henter champion-analyse for alternativet og lader OpenAI udtrække winrate mod enemy laner.
    private void enrichAlternativesWithWinrate(CoachResponse response, CoachRequest coachRequest) {
        if (response.getAlternatives() == null || coachRequest.getEnemyTeam() == null) return;

        String enemyLanerName = findEnemyLaner(coachRequest);
        if (enemyLanerName == null) return;

        for (CoachResponse.Alternative alt : response.getAlternatives()) {
            try {
                String analysisText = opggService.getChampionAnalysis(alt.getName(), coachRequest.getUserRole());
                double winrate = extractWinrateViaOpenAi(analysisText, alt.getName(), enemyLanerName);
                if (winrate > 0) alt.setWinrate(winrate);
            } catch (Exception e) {
                // Fejl i OP.GG-opslag for ét alternativ skal ikke stoppe resten af coachingen.
                logger.warn("Kunne ikke hente winrate for {}: {}", alt.getName(), e.getMessage());
            }
        }
    }

    private String findEnemyLaner(CoachRequest coachRequest) {
        return coachRequest.getEnemyTeam().stream()
                .filter(pick -> pick.getRole().equalsIgnoreCase(coachRequest.getUserRole()))
                .map(DraftPick::getName)
                .findFirst()
                .orElse(null);
    }

    // Bruger OpenAI til at udtrække winrate fra OP.GG-teksten.
    // Returnerer winrate som procent (f.eks. 54.3) eller -1 hvis ikke fundet.
    private double extractWinrateViaOpenAi(String opggText, String championName, String enemyName) {
        if (opggText == null || opggText.isEmpty()) return -1;

        String systemMsg = "Extract the winrate of " + championName + " vs " + enemyName +
                " from the following data. Reply with STRICT JSON only: {\"winrate\": <number>}" +
                " If the data doesn't contain this matchup, reply: {\"winrate\": -1}";

        String truncated = opggText.length() > 500 ? opggText.substring(0, 500) : opggText;

        try {
            String reply = openAiService.chat(systemMsg, truncated);
            WinrateResponse parsed = openAiService.parseJsonReply(reply, WinrateResponse.class);
            return parsed.getWinrate();
        } catch (Exception e) {
            logger.warn("Kunne ikke parse winrate fra OpenAI for {} vs {}", championName, enemyName);
            return -1;
        }
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

}
