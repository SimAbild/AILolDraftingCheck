package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftPickRequest;
import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Service
public class DraftService {

    DataDragonService dataDragonService;
    OpenAiService openAiService;

    private static final List<String> VALID_ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends draft generator." +
                    " Given the user's role, output a realistic draft for the OTHER nine slots:" +
                    " 5 enemy champions and 4 ally champions (exclude the user's role from the ally list)." +
                    " Reply with STRICT JSON only, no markdown, in this exact shape:" +
                    " {\"enemy\":[{\"role\":\"TOP\",\"champion\":\"Aatrox\"}, ...5 entries...]," +
                    " \"ally\":[{\"role\":\"JGL\",\"champion\":\"...\"}, ...4 entries, EXCLUDING user role...]}" +
                    " Use champion names that exist in League of Legends.";

    public DraftService(DataDragonService dataDragonService, OpenAiService openAiService) {
        this.dataDragonService = dataDragonService;
        this.openAiService = openAiService;
    }

    public DraftResponse generateDraft(String role) {
        String userRole = extractAndValidateRole(role);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        try {
            JsonNode responseJson = openAiService.parseAiReply(aiReply);
            List<DraftPickRequest> enemyTeam = buildTeamPicks(responseJson.get("enemy"), null);
            List<DraftPickRequest> allyTeam = buildTeamPicks(responseJson.get("ally"), userRole);
            return new DraftResponse(userRole, enemyTeam, allyTeam);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    public String extractAndValidateRole(String role) {
        String userRole = Optional.ofNullable(role)
                .map(String::toUpperCase)
                .orElse("");
        boolean isValidRole = VALID_ROLES.contains(userRole);
        if (!isValidRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role must be one of " + VALID_ROLES);
        }
        return userRole;
    }

    public List<DraftPickRequest> buildTeamPicks(JsonNode jsonArray, String excludedRole) {
        List<DraftPickRequest> picks = new ArrayList<>();

        if (jsonArray == null || !jsonArray.isArray()) return picks;

        for (JsonNode pickNode : jsonArray) {
            String role = pickNode.path("role").asText("").toUpperCase(Locale.ROOT);
            String championName = pickNode.path("champion").asText("");

            if (shouldSkipPick(role, excludedRole)) continue;

            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(championName);
            if (maybeChampion.isEmpty()) continue;

            picks.add(new DraftPickRequest(role, maybeChampion.get().getName(), maybeChampion.get().getIconUrl()));
        }
        return picks;
    }

    public boolean shouldSkipPick(String role, String excludedRole) {
        boolean isUnknownRole = !VALID_ROLES.contains(role);
        boolean isExcludedRole = excludedRole != null && role.equals(excludedRole);
        return isUnknownRole || isExcludedRole;
    }

}
