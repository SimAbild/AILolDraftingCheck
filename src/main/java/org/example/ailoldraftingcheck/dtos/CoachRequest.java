package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The body the frontend POSTs after the user has chosen a champion.
 * Carries everything the LLM needs to evaluate the pick:
 *  - the user's role and chosen champion
 *  - the rest of the ally team (4 picks)
 *  - the entire enemy team (5 picks)
 */
@Getter
@Setter
@NoArgsConstructor
public class CoachRequest {
    private String userRole;
    private String userChampion;        // championName, e.g. "Soraka"
    private List<DraftPick> allyTeam;   // 4 picks (no user slot)
    private List<DraftPick> enemyTeam;  // 5 picks
}
