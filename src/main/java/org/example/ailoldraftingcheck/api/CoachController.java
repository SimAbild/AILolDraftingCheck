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

/**
 * POST /api/v1/coach
 * Reviews the user's champion pick. Returns positives, negatives, and up to
 * three stronger alternative champions - each with a short reason.
 */
@RestController
@RequestMapping("/api/v1/coach")
@CrossOrigin(origins = "*")
public class CoachController {

    // Instructions for the AI + the exact JSON shape we want back.
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

    private final OpenAiService openAi;
    private final DataDragonService dataDragon;

    public CoachController(OpenAiService openAi, DataDragonService dataDragon) {
        this.openAi = openAi;
        this.dataDragon = dataDragon;
    }

    /**
     * Steps:
     *   1. Validate the request.
     *   2. Build a user prompt listing the full draft.
     *   3. Ask OpenAI for the feedback JSON.
     *   4. Parse it and attach icon URLs to the alternative champions.
     */
    @PostMapping
    public CoachResponse getCoach(@RequestBody CoachRequest req) {

        // 1. Minimal validation.
        if (req.getUserChampion() == null || req.getUserRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userChampion and userRole are required");
        }

        // 2. Build the user prompt the AI will read.
        String userPrompt = buildUserPrompt(req);

        // 3. Call ChatGPT.
        String aiReply = openAi.chat(SYSTEM_MESSAGE, userPrompt);

        // 4. Parse the AI's JSON reply.
        try {
            JsonNode root = parseJson(aiReply);
            List<String> positives = toStrings(root.get("positives"));
            List<String> negatives = toStrings(root.get("negatives"));
            List<CoachResponse.Alternative> alts = parseAlternatives(root.get("alternatives"));
            return new CoachResponse(positives, negatives, alts);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    /** Format the draft as a plain-text prompt for the AI. */
    private String buildUserPrompt(CoachRequest req) {
        return "user_role: " + req.getUserRole() +
               "\nuser_champion: " + req.getUserChampion() +
               "\nally_team (4): " + teamToLine(req.getAllyTeam()) +
               "\nenemy_team (5): " + teamToLine(req.getEnemyTeam());
    }

    private String teamToLine(List<DraftPick> team) {
        if (team == null || team.isEmpty()) return "(none)";
        return team.stream()
                .map(p -> p.getRole() + ":" + p.getChampionName())
                .collect(Collectors.joining(", "));
    }

    /** Same trick as DraftController: strip ``` fences if the model added them. */
    private JsonNode parseJson(String content) throws Exception {
        String clean = content.trim();
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("(?s)```(json)?", "").trim();
        }
        return JsonMapper.builder().build().readTree(clean);
    }

    /** Pull a JSON string array into a Java List<String>. */
    private List<String> toStrings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) return out;
        for (JsonNode n : arr) out.add(n.asText());
        return out;
    }

    /** Parse {champion, reason} entries and fill in iconUrl from Data Dragon. */
    private List<CoachResponse.Alternative> parseAlternatives(JsonNode arr) {
        List<CoachResponse.Alternative> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) return out;

        for (JsonNode n : arr) {
            String name = n.path("champion").asText("");
            String reason = n.path("reason").asText("");

            Optional<Champion> c = dataDragon.byName(name);
            String icon = c.map(Champion::getIconUrl).orElse("");
            String resolvedName = c.map(Champion::getName).orElse(name);

            out.add(new CoachResponse.Alternative(resolvedName, icon, reason));
            if (out.size() >= 3) break;
        }
        return out;
    }
}
