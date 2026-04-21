package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CoachRequest {
    private String userRole;
    private String userChampion;
    private List<DraftPickRequest> allyTeam;
    private List<DraftPickRequest> enemyTeam;
}
