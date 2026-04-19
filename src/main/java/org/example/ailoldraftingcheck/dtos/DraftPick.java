package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DraftPick {
    private String role;
    private String championName;
    private String iconUrl;

    public DraftPick(String role, String championName, String iconUrl) {
        this.role = role;
        this.championName = championName;
        this.iconUrl = iconUrl;
    }
}
