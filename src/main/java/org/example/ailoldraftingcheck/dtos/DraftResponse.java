package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Det komplette draft-svar der sendes til frontend:
// brugerens valgte rolle, fjendehold og allieret hold.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DraftResponse {
    private String userRole;
    private List<DraftPick> enemyTeam;
    private List<DraftPick> allyTeam;
}
