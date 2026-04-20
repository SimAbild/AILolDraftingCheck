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
            List<String> strengths = extractStringList(alternativeNode.get("strengths"));

            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(championName);
            String iconUrl = maybeChampion.map(Champion::getIconUrl).orElse("");
            String resolvedChampionName = maybeChampion.map(Champion::getName).orElse(championName);

            alternatives.add(new CoachResponse.Alternative(resolvedChampionName, iconUrl, reason, strengths));
            if (alternatives.size() >= MAX_ALTERNATIVES) break;
        }
        return alternatives;
    }
}
