package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CoachResponse {
    private List<String> positives;
    private List<String> negatives;
    private List<Alternative> alternatives;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alternative {
        @JsonAlias("champion")
        private String name;
        private String reason;
        private List<String> strengths;
        private double winrate; // Winrate vs enemy laner from OP.GG (e.g. 54.3)
    }
}
