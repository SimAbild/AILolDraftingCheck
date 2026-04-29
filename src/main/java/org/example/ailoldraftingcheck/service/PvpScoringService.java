package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.*;
import org.example.ailoldraftingcheck.dtos.pvp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// Beregner point og coaching-feedback for en spillers champion-valg i PvP-mode.
// Bruger OP.GG-data som input til OpenAI, der returnerer en score fra 0-100.
// Pointberegning: Counterpick (60%), Synergi med allies (20%), Counter mod enemies (20%).

@Service
public class PvpScoringService {

    private static final Logger logger = LoggerFactory.getLogger(PvpScoringService.class);
    private static final int FALLBACK_SCORE = 50;

    private static final String SCORING_SYSTEM_MESSAGE =
            "You are a League of Legends scoring system." +
            " Based on the provided OP.GG statistics, calculate a score from 0-100." +
            " Use these weights: Counterpick (60%), Synergy with ally team (20%), Counter against enemy team (20%)." +
            " Reply with STRICT JSON only: {\"score\": <number>}";

    private final OpenAiService openAiService;
    private final CoachService coachService;
    private final OpggService opggService;

    public PvpScoringService(OpenAiService openAiService, CoachService coachService, OpggService opggService) {
        this.openAiService = openAiService;
        this.coachService = coachService;
        this.opggService = opggService;
    }

    // Beregner resultat for en spiller: point + coaching-feedback.
    // Returnerer 0 point hvis spilleren ikke valgte en champion.
    public PvpPlayerResult calculateResult(PvpGame game, PvpPlayer player) {
        if (player.getChampion() == null) {
            return buildNoPickResult(player);
        }

        CoachResponse feedback = getCoachingFeedback(game, player);
        int points = calculatePoints(game, player);

        return new PvpPlayerResult(
                player.getUsername(), player.getChampion(), points, feedback);
    }

    private PvpPlayerResult buildNoPickResult(PvpPlayer player) {
        CoachResponse emptyFeedback = new CoachResponse(
                List.of(),
                List.of("Du valgte ikke en champion inden for tidsfristen."),
                List.of()
        );
        return new PvpPlayerResult(player.getUsername(), null, 0, emptyFeedback);
    }

    private CoachResponse getCoachingFeedback(PvpGame game, PvpPlayer player) {
        CoachRequest request = new CoachRequest();
        request.setUserRole(game.getAssignedRole());
        request.setUserChampion(player.getChampion());
        request.setAllyTeam(game.getAllyTeam());
        request.setEnemyTeam(game.getEnemyTeam());

        return coachService.analyzeChampionPick(request);
    }

    private int calculatePoints(PvpGame game, PvpPlayer player) {
        String analysisData = opggService.getChampionAnalysis(player.getChampion(), game.getAssignedRole());
        String metaData = opggService.getChampionMetaData(player.getChampion(), game.getAssignedRole());

        String enemyLaner = findEnemyLanerName(game);
        String allyNames = formatTeamNames(game.getAllyTeam());
        String enemyNames = formatTeamNames(game.getEnemyTeam());

        String userMessage = "Champion: " + player.getChampion() + " (" + game.getAssignedRole() + ")\n" +
                "Enemy laner: " + enemyLaner + "\n" +
                "Ally team: " + allyNames + "\n" +
                "Enemy team: " + enemyNames + "\n" +
                "OP.GG analysis data:\n" + truncate(analysisData, 500) + "\n" +
                "OP.GG meta data:\n" + truncate(metaData, 500);

        try {
            String reply = openAiService.chat(SCORING_SYSTEM_MESSAGE, userMessage);
            ScoreResponse score = openAiService.parseJsonReply(reply, ScoreResponse.class);
            return Math.min(100, Math.max(0, score.getScore()));
        } catch (Exception e) {
            logger.error("Kunne ikke beregne point via OpenAI: {}", e.getMessage());
            return FALLBACK_SCORE;
        }
    }

    private String findEnemyLanerName(PvpGame game) {
        return game.getEnemyTeam().stream()
                .filter(pick -> pick.getRole().equals(game.getAssignedRole()))
                .map(DraftPick::getName)
                .findFirst()
                .orElse("Unknown");
    }

    private String formatTeamNames(List<DraftPick> team) {
        return team.stream()
                .map(pick -> pick.getRole() + ":" + pick.getName())
                .collect(Collectors.joining(", "));
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) return "unavailable";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
