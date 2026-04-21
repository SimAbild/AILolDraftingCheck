package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Coach-analysen der sendes til frontend med styrker, svagheder og alternativforslag.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CoachResponse {
    private List<String> positives;
    private List<String> negatives;
    private List<Alternative> alternatives;

    // Repræsenterer ét alternativt champion-forslag med direkte sammenligning til brugerens pick.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alternative {
        private String championName;
        private String iconUrl;
        private String reason;
        private List<String> strengths;
    }
}
