package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Klient → server: spilleren vil finde en modstander.
@Getter
@Setter
@NoArgsConstructor
public class PvpFindMatchRequest {
    private String username;
}
