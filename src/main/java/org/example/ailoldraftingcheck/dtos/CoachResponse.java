package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Structured coaching feedback rendered on the result page.
 *
 *  - positives        : what the pick does well (synergy, counters, strengths)
 *  - negatives        : weaknesses, gaps, how the enemy will exploit it
 *  - alternatives     : up to 3 better champion suggestions, each with a short reason
 *  - dataSource       : "op.gg-scrape" when live data was fetched, "llm-only" otherwise
 *                       (so the UI can be honest about how the answer was produced)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoachResponse {
    private List<String> positives;
    private List<String> negatives;
    private List<Alternative> alternatives;
    private String dataSource;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alternative {
        private String championName;
        private String iconUrl;
        private String reason;
    }
}
