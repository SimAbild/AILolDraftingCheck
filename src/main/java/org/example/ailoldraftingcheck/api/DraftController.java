package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.DraftPickRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/v1/draft")
@CrossOrigin(origins = "*")
public class DraftController {

    private static final List<String> VALID_ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends draft generator." +
                    " Given the user's role, output a realistic draft for the OTHER nine slots:" +
                    " 5 enemy champions and 4 ally champions (exclude the user's role from the ally list)." +
                    " Reply with STRICT JSON only, no markdown, in this exact shape:" +
                    " {\"enemy\":[{\"role\":\"TOP\",\"champion\":\"Aatrox\"}, ...5 entries...]," +
                    " \"ally\":[{\"role\":\"JGL\",\"champion\":\"...\"}, ...4 entries, EXCLUDING user role...]}" +
                    " Use champion names that exist in League of Legends.";

    private final OpenAiService openAiService;
    private final DataDragonService dataDragonService;

    public DraftController(OpenAiService openAiService, DataDragonService dataDragonService) {
        this.openAiService = openAiService;
        this.dataDragonService = dataDragonService;
    }

    @PostMapping
    public DraftResponse generateDraft(@RequestBody Map<String, String> requestBody) {
        String userRole = extractAndValidateRole(requestBody);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        try {
            JsonNode responseJson = parseAiReply(aiReply);
            List<DraftPickRequest> enemyTeam = buildTeamPicks(responseJson.get("enemy"), null);
            List<DraftPickRequest> allyTeam = buildTeamPicks(responseJson.get("ally"), userRole);
            return new DraftResponse(userRole, enemyTeam, allyTeam);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private String extractAndValidateRole(Map<String, String> requestBody) {
        String userRole = Optional.ofNullable(requestBody.get("role"))
                .map(String::toUpperCase)
                .orElse("");
        boolean isValidRole = VALID_ROLES.contains(userRole);
        if (!isValidRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role must be one of " + VALID_ROLES);
        }
        return userRole;
    }

    private JsonNode parseAiReply(String content) throws Exception {
        String cleanContent = content.trim();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.replaceAll("(?s)```(json)?", "").trim();
        }
        return JsonMapper.builder().build().readTree(cleanContent);
    }

    private List<DraftPickRequest> buildTeamPicks(JsonNode jsonArray, String excludedRole) {
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

    private boolean shouldSkipPick(String role, String excludedRole) {
        boolean isUnknownRole = !VALID_ROLES.contains(role);
        boolean isExcludedRole = excludedRole != null && role.equals(excludedRole);
        return isUnknownRole || isExcludedRole;
    }
}
