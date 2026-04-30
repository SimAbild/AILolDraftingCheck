package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Server → klient: bruges til både reveal (champion-valg) og results (point + feedback).
// Ved reveal er points=0 og feedback=null — ved results er de udfyldt.
@Getter
@AllArgsConstructor
public class PvpRoundMessage {
    private PvpPlayerResult player1;
    private PvpPlayerResult player2;
}
