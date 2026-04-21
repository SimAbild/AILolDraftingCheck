package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPickRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CoachService {

    private final DataDragonService dataDragonService;
    private final OpenAiService openAiService;

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends drafting coach." +
                    " You will receive the user's role + chosen champion, their 4 ally picks, and the 5 enemy picks." +
                    " Use champion knowledge about synergies with the ally team and counters to the enemy team." +
                    " Reply with STRICT JSON only, no markdown, in this exact shape:" +
                    " {\"positives\":[\"short sentence 1\",\"short sentence 2\",\"short sentence 3\"]," +
                    " \"negatives\":[\"short sentence 1\",\"short sentence 2\",\"short sentence 3\"]," +
                    " \"alternatives\":[" +
                    " {\"champion\":\"Name\",\"reason\":\"one short sentence\"}," +
                    " {\"champion\":\"Name\",\"reason\":\"...\"}," +
                    " {\"champion\":\"Name\",\"reason\":\"...\"}]}" +
                    " Keep each bullet short. Alternatives must be real League champions that fit the user's role.";

    private static final int MAX_ALTERNATIVES = 3;

    public CoachService(DataDragonService dataDragonService, OpenAiService openAiService) {
        this.dataDragonService = dataDragonService;
        this.openAiService = openAiService;
    }

    public CoachResponse analyzeChampionPick(CoachRequest coachRequest) {

        String userPrompt = buildUserPrompt(coachRequest);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, userPrompt);

        try {
            JsonNode responseJson = openAiService.parseAiReply(aiReply);
            List<String> positives = extractStringList(responseJson.get("positives"));
            List<String> negatives = extractStringList(responseJson.get("negatives"));
            List<CoachResponse.Alternative> alternatives = parseChampionAlternatives(responseJson.get("alternatives"));
            return new CoachResponse(positives, negatives, alternatives);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    public String buildUserPrompt(CoachRequest coachRequest) {
        return "user_role: " + coachRequest.getUserRole() +
                "\nuser_champion: " + coachRequest.getUserChampion() +
                "\nally_team (4): " + formatTeamForPrompt(coachRequest.getAllyTeam()) +
                "\nenemy_team (5): " + formatTeamForPrompt(coachRequest.getEnemyTeam());
    }

    public String formatTeamForPrompt(List<DraftPickRequest> team) {
        if (team == null || team.isEmpty()) return "(none)";
        return team.stream()
                .map(pick -> pick.getRole() + ":" + pick.getChampionName())
                .collect(Collectors.joining(", "));
    }

    public List<String> extractStringList(JsonNode jsonArray) {
        List<String> result = new ArrayList<>();
        if (jsonArray == null || !jsonArray.isArray()) return result;
        for (JsonNode jsonNode : jsonArray) result.add(jsonNode.asText());
        return result;
    }

    public List<CoachResponse.Alternative> parseChampionAlternatives(JsonNode jsonArray) {
        List<CoachResponse.Alternative> alternatives = new ArrayList<>();
        if (jsonArray == null || !jsonArray.isArray()) return alternatives;

        for (JsonNode alternativeNode : jsonArray) {
            String championName = alternativeNode.path("champion").asText("");
            String reason = alternativeNode.path("reason").asText("");

            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(championName);
            String iconUrl = maybeChampion.map(Champion::getIconUrl).orElse("");
            String resolvedChampionName = maybeChampion.map(Champion::getName).orElse(championName);

            alternatives.add(new CoachResponse.Alternative(resolvedChampionName, iconUrl, reason));
            if (alternatives.size() >= MAX_ALTERNATIVES) break;
        }
        return alternatives;
    }
}
