package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPickRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CoachService {

    private final DataDragonService dataDragonService;

    private static final int MAX_ALTERNATIVES = 3;

    public CoachService(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
    }

    public boolean isMissingRequiredFields(CoachRequest coachRequest) {
        return coachRequest.getUserChampion() == null || coachRequest.getUserRole() == null;
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

    public JsonNode parseAiReply(String content) throws Exception {
        String cleanContent = content.trim();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.replaceAll("(?s)```(json)?", "").trim();
        }
        return JsonMapper.builder().build().readTree(cleanContent);
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
