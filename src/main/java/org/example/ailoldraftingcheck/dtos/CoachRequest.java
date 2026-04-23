package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Forespørgslen fra frontend til coach-analysen.
// Deserialiseres fra HTTP request body — @NoArgsConstructor og @Getter
// er påkrævet for at Jackson kan læse JSON-felterne.
// @JsonProperty gør det eksplicit hvilke JSON-feltnavne der mappes hertil.
@Getter
@NoArgsConstructor
public class CoachRequest {

    @JsonProperty("userRole")
    private String userRole;

    @JsonProperty("userChampion")
    private String userChampion;

    @JsonProperty("allyTeam")
    private List<DraftPickRequest> allyTeam;

    @JsonProperty("enemyTeam")
    private List<DraftPickRequest> enemyTeam;
}
