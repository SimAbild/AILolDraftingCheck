package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.AiCoachChampAlternative;
import org.example.ailoldraftingcheck.dtos.AiCoachAnalysis;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoachService {

    private static final int MAX_ALTERNATIVES = 3;

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends drafting coach." +
            " You receive the user's role, chosen champion, their 4 ally picks, and the 5 enemy picks." +

            " RULE 1 — OFF-META DETECTION:" +
            " If the chosen champion is unconventional or off-meta for the given role" +
            " (e.g. Milio mid, Yuumi jungle, Nasus support)," +
            " the FIRST negative MUST explicitly state this:" +
            " name the role, name the champion, and explain in one concrete sentence why it does not work there." +

            " RULE 2 — ALTERNATIVES MUST COMPARE, NOT DESCRIBE:" +
            " Each alternative must be compared DIRECTLY to the user's chosen champion." +
            " Do NOT write generic descriptions of the alternative." +
            " Write what makes it concretely different and better in this specific draft." +
            " This is especially important when alternatives are similar champions" +
            " (e.g. Lulu vs Milio): name the exact mechanical or statistical difference." +

            " Reply with STRICT JSON only, no markdown, in this exact shape:" +
            " {\"positives\":[\"sentence 1\",\"sentence 2\",\"sentence 3\"]," +
            " \"negatives\":[\"sentence 1\",\"sentence 2\",\"sentence 3\"]," +
            " \"alternatives\":[" +
            " {\"champion\":\"Name\"," +
            " \"reason\":\"One sentence that directly compares this champion to [user's champion] and explains why it fits this draft better\"," +
            " \"strengths\":[" +
            " \"Unlike [user's champion], [alternative] can [specific mechanical difference vs this enemy or ally]\"," +
            " \"Counters [specific enemy champion] better than [user's champion] because [concrete ability or stat]\"," +
            " \"[One concrete edge in this draft, e.g. higher peel, more engage, better wave clear]\"" +
            " ]}," +
            " {\"champion\":\"Name\",\"reason\":\"...\",\"strengths\":[\"...\",\"...\",\"...\"]}," +
            " {\"champion\":\"Name\",\"reason\":\"...\",\"strengths\":[\"...\",\"...\",\"...\"]}]}" +
            " Each strengths array must contain exactly 3 entries." +
            " Name specific champions, abilities, and mechanics — never write vague generalities." +
            " Alternatives must be real League of Legends champions that fit the user's role.";

    private final OpenAiService openAiService;
    private final DataDragonService dataDragonService;

    public CoachService(OpenAiService openAiService, DataDragonService dataDragonService) {
        this.openAiService = openAiService;
        this.dataDragonService = dataDragonService;
    }

    public CoachResponse analyzeChampionPick(CoachRequest coachRequest) {
        String userPrompt = buildUserPrompt(coachRequest);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, userPrompt);

        try {
            // Jackson deserialiserer JSON-svaret direkte til AiCoachAnalysis,
            // inklusiv de indlejrede lister af strings og AiChampionRecommendation-objekter.
            AiCoachAnalysis aiCoach = openAiService.parseJsonReply(aiReply, AiCoachAnalysis.class);
            List<CoachResponse.Alternative> alternatives = resolveChampionAlternatives(aiCoach.getAlternatives());
            return new CoachResponse(aiCoach.getPositives(), aiCoach.getNegatives(), alternatives);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private String buildUserPrompt(CoachRequest coachRequest) {
        return "user_role: " + coachRequest.getUserRole() +
               "\nuser_champion: " + coachRequest.getUserChampion() +
               "\nally_team (4): " + formatTeamForPrompt(coachRequest.getAllyTeam()) +
               "\nenemy_team (5): " + formatTeamForPrompt(coachRequest.getEnemyTeam());
    }

    private String formatTeamForPrompt(List<DraftPick> team) {
        if (team == null || team.isEmpty()) return "(none)";
        return team.stream()
                .map(pick -> pick.getRole() + ":" + pick.getChampionName())
                .collect(Collectors.joining(", "));
    }

    private List<CoachResponse.Alternative> resolveChampionAlternatives(List<AiCoachChampAlternative> aiRecommendations) {
        if (aiRecommendations == null) return List.of();

        List<CoachResponse.Alternative> alternatives = new ArrayList<>();
        for (AiCoachChampAlternative aiAlternative : aiRecommendations) {
            // Optional bruges her fordi en champion måske ikke kendes i Data Dragon.
            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(aiAlternative.getChampionName());
            String iconUrl = maybeChampion.map(Champion::getIconUrl).orElse("");
            String resolvedName = maybeChampion.map(Champion::getName).orElse(aiAlternative.getChampionName());

            alternatives.add(new CoachResponse.Alternative(resolvedName, iconUrl, aiAlternative.getReason(), aiAlternative.getStrengths()));
            if (alternatives.size() >= MAX_ALTERNATIVES) break;
        }
        return alternatives;
    }
}
