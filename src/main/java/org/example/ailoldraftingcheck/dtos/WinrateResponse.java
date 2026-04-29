package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// OpenAI returnerer dette JSON-objekt når den udtrækker winrate fra OP.GG-data.
// Eksempel: {"winrate": 54.3} eller {"winrate": -1} hvis matchuppet ikke findes.
@Getter
@Setter
@NoArgsConstructor
public class WinrateResponse {
    private double winrate;
}
