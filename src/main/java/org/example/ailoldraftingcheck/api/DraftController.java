package org.example.ailoldraftingcheck.api;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * POST /api/v1/draft
 * Asks ChatGPT to generate a realistic draft around the user's chosen role:
 * 5 enemy champions and 4 ally champions (the user's role slot is left empty
 * so they can pick it themselves on the next screen).
 */
@RestController
@RequestMapping("/api/v1/draft")
@CrossOrigin(origins = "*")
public class DraftController {

    // The five valid roles.
    private static final List<String> ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    // Tells the AI exactly how to behave and how to format its answer.
    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends draft generator." +
            " Given the user's role, output a realistic draft for the OTHER nine slots:" +
            " 5 enemy champions and 4 ally champions (exclude the user's role from the ally list)." +
            " Reply with STRICT JSON only, no markdown, in this exact shape:" +
            " {\"enemy\":[{\"role\":\"TOP\",\"champion\":\"Aatrox\"}, ...5 entries...]," +
            " \"ally\":[{\"role\":\"JGL\",\"champion\":\"...\"}, ...4 entries, EXCLUDING user role...]}" +
            " Use champion names that exist in League of Legends.";

    private final OpenAiService openAi;
    private final DataDragonService dataDragon;

    public DraftController(OpenAiService openAi, DataDragonService dataDragon) {
        this.openAi = openAi;
        this.dataDragon = dataDragon;
    }

    /**
     * Steps:
     *   1. Validate the incoming role.
     *   2. Ask OpenAI to produce the draft as JSON.
     *   3. Parse that JSON and look up each champion in Data Dragon to get icons.
     *   4. Return a clean DraftResponse.
     */
    @PostMapping
    public DraftResponse getDraft(@RequestBody Map<String, String> body) {

        // 1. Read and validate "role" from the request body.
        String userRole = Optional.ofNullable(body.get("role"))
                .map(String::toUpperCase)
                .orElse("");
        if (!ROLES.contains(userRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role must be one of " + ROLES);
        }

        // 2. Call ChatGPT.
        String aiReply = openAi.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        // 3. Parse the AI's JSON reply into DraftPick lists.
        try {
            JsonNode root = parseJson(aiReply);
            List<DraftPick> enemy = parseTeam(root.get("enemy"), null);
            List<DraftPick> ally = parseTeam(root.get("ally"), userRole);
            // 4. Return the structured response to the frontend.
            return new DraftResponse(userRole, enemy, ally);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    /** ChatGPT sometimes wraps JSON in ``` code fences - strip them before parsing. */
    private JsonNode parseJson(String content) throws Exception {
        String clean = content.trim();
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("(?s)```(json)?", "").trim();
        }
        return JsonMapper.builder().build().readTree(clean);
    }

    /**
     * Turn the AI's array of {role, champion} into DraftPick objects.
     * Unknown champions or entries for the excluded role are skipped.
     */
    private List<DraftPick> parseTeam(JsonNode arr, String excludedRole) {
        List<DraftPick> picks = new ArrayList<>();
        if (arr == null || !arr.isArray()) return picks;

        for (JsonNode n : arr) {
            String role = n.path("role").asText("").toUpperCase(Locale.ROOT);
            String champ = n.path("champion").asText("");

            if (!ROLES.contains(role)) continue;
            if (excludedRole != null && role.equals(excludedRole)) continue;

            Optional<Champion> c = dataDragon.byName(champ);
            if (c.isEmpty()) continue;

            picks.add(new DraftPick(role, c.get().getName(), c.get().getIconUrl()));
        }
        return picks;
    }
}
