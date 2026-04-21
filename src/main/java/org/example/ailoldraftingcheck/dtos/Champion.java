package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Repræsenterer en LoL-champion hentet fra Riot's Data Dragon API.
// Sendes til frontend som JSON — Jackson bruger @Getter til serialisering.
// @NoArgsConstructor er påkrævet for Jackson-deserialisering.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Champion {
    private String id;
    private String name;
    private String iconUrl;
}
