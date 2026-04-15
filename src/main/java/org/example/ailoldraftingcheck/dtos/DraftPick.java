package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One slot in a team: which role, which champion, and the icon URL.
 */
@Getter
@Setter
@NoArgsConstructor
public class DraftPick {
    private String role;          // TOP / JGL / MID / ADC / SUPP
    private String championName;  // e.g. "Aatrox"
    private String iconUrl;       // square champion icon

    public DraftPick(String role, String championName, String iconUrl) {
        this.role = role;
        this.championName = championName;
        this.iconUrl = iconUrl;
    }
}
