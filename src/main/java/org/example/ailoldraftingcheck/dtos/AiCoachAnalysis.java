package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Repræsenterer den fulde coach-analyse returneret af AI'en.
// Jackson mapper JSON-feltnavnene direkte til klassens felter:
//   "positives"    → List<String>
//   "negatives"    → List<String>
//   "alternatives" → List<AiChampionRecommendation>
@Getter
@Setter
@NoArgsConstructor
public class AiCoachAnalysis {
    private List<String> positives;
    private List<String> negatives;
    private List<AiChampionRecommendation> alternatives;
}
