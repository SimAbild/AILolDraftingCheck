package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CoachResponse {
    private List<String> positives;
    private List<String> negatives;
    private List<Alternative> alternatives;

    public CoachResponse(List<String> positives, List<String> negatives, List<Alternative> alternatives) {
        this.positives = positives;
        this.negatives = negatives;
        this.alternatives = alternatives;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Alternative {
        private String championName;
        private String iconUrl;
        private String reason;
        private List<String> strengths;

        public Alternative(String championName, String iconUrl, String reason, List<String> strengths) {
            this.championName = championName;
            this.iconUrl = iconUrl;
            this.reason = reason;
            this.strengths = strengths;
        }
    }
}
