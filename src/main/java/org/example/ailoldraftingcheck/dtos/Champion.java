package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One League of Legends champion as exposed by Riot Data Dragon.
 *   id      - internal id, e.g. "MonkeyKing"
 *   name    - display name, e.g. "Wukong"
 *   iconUrl - full square icon URL
 */
@Getter
@Setter
@NoArgsConstructor
public class Champion {
    private String id;
    private String name;
    private String iconUrl;

    public Champion(String id, String name, String iconUrl) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
    }
}
