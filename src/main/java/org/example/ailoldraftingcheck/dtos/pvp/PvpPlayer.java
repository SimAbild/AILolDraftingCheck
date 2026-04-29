package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// Repræsenterer en spiller i PvP-matchmaking og aktive spil.
// sessionId er server-internt (sendes aldrig til klienten).
@Getter
@Setter
@AllArgsConstructor
public class PvpPlayer {
    private String sessionId;
    private String username;
    private String champion;
}
