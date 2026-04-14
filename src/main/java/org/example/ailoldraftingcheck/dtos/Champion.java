package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A League of Legends champion as exposed by Riot Data Dragon.
 *  - id      : the Data Dragon ID (e.g. "MonkeyKing", "Aatrox") - used to build icon URL
 *  - name    : the user-facing name (e.g. "Wukong", "Aatrox")
 *  - iconUrl : full URL to the square champion icon, hosted by Riot
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Champion {
    private String id;
    private String name;
    private String iconUrl;
}
