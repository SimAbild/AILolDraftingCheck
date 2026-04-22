package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Repræsenterer ét champion-valg fra AI'ens draft-svar.
// Deserialiseres udelukkende fra AI's JSON — @NoArgsConstructor og @Setter
// er påkrævet for at Jackson kan sætte felterne efter oprettelse.
// @JsonProperty gør det eksplicit hvilke JSON-feltnavne der mappes hertil.
@Getter
@Setter
@NoArgsConstructor
public class AiDraftChampionEntry {

    @JsonProperty("role")
    private String role;

    @JsonProperty("champion")
    private String champion;
}
