package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Repræsenterer det fulde JSON-svar fra AI'en ved draft-generering:
// et matchup bestående af fjendehold og allieret hold.
// Jackson mapper JSON-arrayene "enemy" og "ally" til lister af AiChampionEntry.
@Getter
@Setter
@NoArgsConstructor
public class AiMatchup {
    private List<AiChampionEntry> enemy;
    private List<AiChampionEntry> ally;
}
