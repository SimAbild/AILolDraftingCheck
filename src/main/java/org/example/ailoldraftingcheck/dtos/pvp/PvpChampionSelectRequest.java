package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Klient → server: spilleren har valgt en champion.
@Getter
@Setter
@NoArgsConstructor
public class PvpChampionSelectRequest {
    private String roomId;
    private String champion;
}
