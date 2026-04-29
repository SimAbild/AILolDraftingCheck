package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Server → klient: viser begge spilleres champion-valg efter countdown.
// Bruger PvpPlayerResult med points=0 og feedback=null (endnu ikke beregnet).
@Getter
@AllArgsConstructor
public class PvpRevealMessage {
    private PvpPlayerResult player1;
    private PvpPlayerResult player2;
}
