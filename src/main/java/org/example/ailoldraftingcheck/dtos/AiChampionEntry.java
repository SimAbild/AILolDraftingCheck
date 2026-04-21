package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Repræsenterer ét champion-valg fra AI'ens draft-svar.
// Deserialiseres udelukkende fra AI's JSON — @NoArgsConstructor og @Setter
// er påkrævet for at Jackson kan sætte felterne efter oprettelse.
@Getter
@Setter
@NoArgsConstructor
public class AiChampionEntry {
    private String role;
    private String champion;
}
