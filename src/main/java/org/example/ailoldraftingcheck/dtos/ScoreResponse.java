package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// OpenAI returnerer dette JSON-objekt når den beregner point.
// Eksempel: {"score": 72}
@Getter
@Setter
@NoArgsConstructor
public class ScoreResponse {
    private int score;
}
