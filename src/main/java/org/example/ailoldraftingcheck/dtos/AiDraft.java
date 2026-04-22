package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Repræsenterer det fulde JSON-svar fra AI'en ved draft-generering:
// et matchup bestående af fjendehold og allieret hold.
// @JsonProperty gør det eksplicit hvilke JSON-feltnavne der mappes hertil.
@Getter
@Setter
@NoArgsConstructor
public class AiDraft {

    @JsonProperty("enemy")
    private List<AiDraftChampionEntry> enemy;

    @JsonProperty("ally")
    private List<AiDraftChampionEntry> ally;
}
