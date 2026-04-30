package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.pvp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

// Beregner resultat for en spillers champion-valg i PvP-mode.
// Bruger CoachService til al analyse: feedback, winrate og score.

@Service
public class PvpScoringService {

    private static final Logger logger = LoggerFactory.getLogger(PvpScoringService.class);

    private final CoachService coachService;

    public PvpScoringService(CoachService coachService) {
        this.coachService = coachService;
    }

    // Beregner resultat for en spiller: point + coaching-feedback.
    // Returnerer 0 point hvis spilleren ikke valgte en champion.
    public PvpPlayerResult calculateResult(PvpGame game, PvpPlayer player) {
        if (player.getChampion() == null) {
            return buildNoPickResult(player);
        }

        CoachResponse coachResponse = getCoachAnalysis(game, player);
        int points = coachResponse.getScore() != null ? coachResponse.getScore() : 0;

        return new PvpPlayerResult(player.getUsername(), player.getChampion(), points, coachResponse);
    }

    private PvpPlayerResult buildNoPickResult(PvpPlayer player) {
        CoachResponse emptyFeedback = new CoachResponse(
                List.of(),
                List.of("Du valgte ikke en champion inden for tidsfristen."),
                List.of(),
                null,
                null
        );
        return new PvpPlayerResult(player.getUsername(), null, 0, emptyFeedback);
    }

    private CoachResponse getCoachAnalysis(PvpGame game, PvpPlayer player) {
        CoachRequest request = new CoachRequest();
        request.setUserRole(game.getAssignedRole());
        request.setUserChampion(player.getChampion());
        request.setAllyTeam(game.getAllyTeam());
        request.setEnemyTeam(game.getEnemyTeam());

        return coachService.analyzeChampionPick(request);
    }
}
