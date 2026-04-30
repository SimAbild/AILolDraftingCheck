package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoachResponse {
    private List<String> positives;
    private List<String> negatives;
    private List<Alternative> alternatives;
    private Double matchupWinrate; // Brugerens champion winrate vs enemy laner fra OP.GG
    private Integer score;         // 0-100 score baseret på counterpick, synergi og counters

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alternative {
        @JsonAlias("champion")
        private String name;
        private String reason;
        private List<String> strengths;
        private Double winrate; // Winrate vs enemy laner fra OP.GG (f.eks. 54.3) — null indtil OP.GG-data er hentet
    }
}
