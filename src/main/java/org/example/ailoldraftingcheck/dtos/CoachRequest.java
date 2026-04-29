package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Deserialiseres fra HTTP request body (solo-mode) eller bygges programmatisk (PvP-mode).
// @Setter tilføjet så PvpGameService kan oprette requests uden JSON-hacks.
@Getter
@Setter
@NoArgsConstructor
public class CoachRequest {

    @JsonProperty("userRole")
    private String userRole;

    @JsonProperty("userChampion")
    private String userChampion;

    @JsonProperty("allyTeam")
    private List<DraftPick> allyTeam;

    @JsonProperty("enemyTeam")
    private List<DraftPick> enemyTeam;
}
