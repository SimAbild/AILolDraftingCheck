package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.Getter;
import lombok.Setter;
import org.example.ailoldraftingcheck.dtos.DraftPick;

import java.util.List;

// Holder al state for et aktivt PvP-spil på serveren.
// Oprettes når to spillere matches, og fjernes når resultater er sendt.
@Getter
@Setter
public class PvpGame {
    private String roomId;
    private PvpPlayer player1;
    private PvpPlayer player2;
    private String assignedRole;
    private List<DraftPick> enemyTeam;
    private List<DraftPick> allyTeam;
    private boolean countdownFinished;

    public PvpGame(String roomId, PvpPlayer player1, PvpPlayer player2, String assignedRole) {
        this.roomId = roomId;
        this.player1 = player1;
        this.player2 = player2;
        this.assignedRole = assignedRole;
        this.countdownFinished = false;
    }

    public PvpPlayer getPlayerBySessionId(String sessionId) {
        if (player1.getSessionId().equals(sessionId)) return player1;
        if (player2.getSessionId().equals(sessionId)) return player2;
        return null;
    }
}
