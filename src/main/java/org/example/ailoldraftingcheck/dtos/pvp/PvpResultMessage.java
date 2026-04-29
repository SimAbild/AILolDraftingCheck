package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Server → klient: endelige resultater med point og coaching-feedback.
@Getter
@AllArgsConstructor
public class PvpResultMessage {
    private PvpPlayerResult player1;
    private PvpPlayerResult player2;
}
