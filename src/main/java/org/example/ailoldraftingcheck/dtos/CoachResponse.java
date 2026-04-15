package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * What the /coach endpoint returns.
 *   positives    - short sentences about what works
 *   negatives    - short sentences about what doesn't
 *   alternatives - up to 3 stronger champion suggestions with reasons
 */
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

    /** One alternative champion suggested by the coach. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Alternative {
        private String championName;
        private String iconUrl;
        private String reason;

        public Alternative(String championName, String iconUrl, String reason) {
            this.championName = championName;
            this.iconUrl = iconUrl;
            this.reason = reason;
        }
    }
}
