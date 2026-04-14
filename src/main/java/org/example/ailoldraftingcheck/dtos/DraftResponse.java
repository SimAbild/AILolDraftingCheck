package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * What the /draft endpoint returns.
 *   userRole   - echoed back so the frontend knows which ally slot is empty
 *   enemyTeam  - 5 picks
 *   allyTeam   - 4 picks (excludes user's role)
 */
@Getter
@Setter
@NoArgsConstructor
public class DraftResponse {
    private String userRole;
    private List<DraftPick> enemyTeam;
    private List<DraftPick> allyTeam;

    public DraftResponse(String userRole, List<DraftPick> enemyTeam, List<DraftPick> allyTeam) {
        this.userRole = userRole;
        this.enemyTeam = enemyTeam;
        this.allyTeam = allyTeam;
    }
}
