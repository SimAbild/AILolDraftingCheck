package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.ailoldraftingcheck.dtos.DraftPick;

import java.util.List;

// Server → klient: et match er fundet, her er draftet.
@Getter
@AllArgsConstructor
public class PvpMatchFoundMessage {
    private String roomId;
    private String role;
    private List<DraftPick> enemyTeam;
    private List<DraftPick> allyTeam;
    private String player1Username;
    private String player2Username;
}
