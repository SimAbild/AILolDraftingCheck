package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Forespørgslen fra frontend til coach-analysen.
// Deserialiseres fra HTTP request body — @NoArgsConstructor og @Getter
// er påkrævet for at Jackson kan læse JSON-felterne.
@Getter
@NoArgsConstructor
public class CoachRequest {
    private String userRole;
    private String userChampion;
    private List<DraftPick> allyTeam;
    private List<DraftPick> enemyTeam;
}
