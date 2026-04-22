package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Repræsenterer den fulde coach-analyse returneret af AI'en.
// @JsonProperty gør det eksplicit hvilke JSON-feltnavne der mappes hertil:
//   "positives"    → List<String>
//   "negatives"    → List<String>
//   "alternatives" → List<AiChampionRecommendation>
@Getter
@Setter
@NoArgsConstructor
public class AiCoachAnalysis {

    @JsonProperty("positives")
    private List<String> positives;

    @JsonProperty("negatives")
    private List<String> negatives;

    @JsonProperty("alternatives")
    private List<AiCoachChampAlternative> alternatives;
}
