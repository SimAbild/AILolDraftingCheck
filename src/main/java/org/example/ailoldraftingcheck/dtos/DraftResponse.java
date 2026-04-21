package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DraftResponse {
    private String userRole;
    private List<DraftPickRequest> enemyTeam;
    private List<DraftPickRequest> allyTeam;

    public DraftResponse(String userRole, List<DraftPickRequest> enemyTeam, List<DraftPickRequest> allyTeam) {
        this.userRole = userRole;
        this.enemyTeam = enemyTeam;
        this.allyTeam = allyTeam;
    }
}
