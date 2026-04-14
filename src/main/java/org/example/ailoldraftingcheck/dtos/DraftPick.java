package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single slot in a team draft: which role and which champion is in it.
 * Used inside DraftResponse for both the enemy team and the user's ally team.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DraftPick {
    private String role;          // TOP, JGL, MID, ADC, SUPP
    private String championId;    // Data Dragon id, e.g. "Aatrox"
    private String championName;  // user-facing name, e.g. "Aatrox"
    private String iconUrl;       // full square icon URL
}
