package org.example.ailoldraftingcheck.api;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coach")
@CrossOrigin(origins = "*")
public class CoachController {

    private static final int MAX_ALTERNATIVES = 3;

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

    private final OpenAiService openAiService;
    private final DataDragonService dataDragonService;

    public CoachController(OpenAiService openAiService, DataDragonService dataDragonService) {
        this.openAiService = openAiService;
        this.dataDragonService = dataDragonService;
    }

    @PostMapping
    public CoachResponse analyzeChampionPick(@RequestBody CoachRequest coachRequest) {
        if (isMissingRequiredFields(coachRequest)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userChampion and userRole are required");
        }

        String userPrompt = buildUserPrompt(coachRequest);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, userPrompt);

        try {
            JsonNode responseJson = parseAiReply(aiReply);
            List<String> positives = extractStringList(responseJson.get("positives"));
            List<String> negatives = extractStringList(responseJson.get("negatives"));
            List<CoachResponse.Alternative> alternatives = parseChampionAlternatives(responseJson.get("alternatives"));
            return new CoachResponse(positives, negatives, alternatives);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private boolean isMissingRequiredFields(CoachRequest coachRequest) {
        return coachRequest.getUserChampion() == null || coachRequest.getUserRole() == null;
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

    private JsonNode parseAiReply(String content) throws Exception {
        String cleanContent = content.trim();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.replaceAll("(?s)```(json)?", "").trim();
        }
        return JsonMapper.builder().build().readTree(cleanContent);
    }

    private List<String> extractStringList(JsonNode jsonArray) {
        List<String> result = new ArrayList<>();
        if (jsonArray == null || !jsonArray.isArray()) return result;
        for (JsonNode jsonNode : jsonArray) result.add(jsonNode.asText());
        return result;
    }

    private List<CoachResponse.Alternative> parseChampionAlternatives(JsonNode jsonArray) {
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
