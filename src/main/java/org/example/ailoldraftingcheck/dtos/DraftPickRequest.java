package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DraftPickRequest {
    private String role;
    private String championName;
    private String iconUrl;

    public DraftPickRequest(String role, String championName, String iconUrl) {
        this.role = role;
        this.championName = championName;
        this.iconUrl = iconUrl;
    }
}
