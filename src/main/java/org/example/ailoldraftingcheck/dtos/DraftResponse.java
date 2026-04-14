package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * What the AI generates when the user picks their role on the front page.
 *  - userRole  : echoed back so the frontend knows which slot is empty
 *  - enemyTeam : 5 champions
 *  - allyTeam  : 4 champions (the user's role slot is omitted - they pick it themselves)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DraftResponse {
    private String userRole;
    private List<DraftPick> enemyTeam;
    private List<DraftPick> allyTeam;
}
