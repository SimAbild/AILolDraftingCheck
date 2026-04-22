package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Repræsenterer én anbefalet alternativ champion fra AI'ens coach-analyse.
// Deserialiseres udelukkende fra AI's JSON.
//
// @JsonProperty bruges når JSON-feltnavnet og Java-feltnavnet er forskellige.
// Her sender AI'en feltet som "champion" i JSON:
//   { "champion": "Lulu", "reason": "...", "strengths": [...] }
//
// Men i Java ønsker vi et mere beskrivende navn: championName.
// @JsonProperty("champion") fortæller Jackson:
//   "når du ser feltet 'champion' i JSON, skal det mappes til dette Java-felt."
//
// Uden @JsonProperty ville Jackson lede efter et felt kaldet "championName"
// i JSON — og ikke finde det, fordi AI'en kalder det "champion".
@Getter
@Setter
@NoArgsConstructor
public class AiCoachChampAlternative {

    @JsonProperty("champion")
    private String championName;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("strengths")
    private List<String> strengths;
}
