package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * The body the frontend POSTs to /coach after the user has chosen a champion.
 */
@Getter
@Setter
@NoArgsConstructor
public class CoachRequest {
    private String userRole;
    private String userChampion;
    private List<DraftPick> allyTeam;
    private List<DraftPick> enemyTeam;
}
