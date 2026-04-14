package org.example.ailoldraftingcheck.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Trin 2 in the flow: the AI generates the enemy team (5 picks) and the user's
 * ally team (4 picks - the user's role slot is left empty so they fill it).
 *
 * The LLM is constrained to return strict JSON. We then resolve every champion
 * name through Data Dragon to attach official IDs and icon URLs. If the LLM
 * hallucinates a champion that doesn't exist, we drop it and fill from the
 * unused champion pool so the team is always 5/4.
 */
@RestController
@RequestMapping("/api/v1/draft")
@CrossOrigin(origins = "*")
public class DraftController {

    private static final Logger logger = LoggerFactory.getLogger(DraftController.class);
    private static final List<String> ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    private static final String SYSTEM_MESSAGE = """
            You are a League of Legends draft generator.
            Given a user's main role, output a realistic draft (current meta, no duplicates between teams)
            for the OTHER 9 slots: 5 enemy champions and 4 ally champions (the user's role slot is left
            for them to fill themselves).
            Respond with STRICT JSON ONLY, no markdown, no explanations, with this exact shape:
            {
              "enemy": [{"role":"TOP","champion":"Aatrox"}, {"role":"JGL","champion":"..."},
                        {"role":"MID","champion":"..."}, {"role":"ADC","champion":"..."},
                        {"role":"SUPP","champion":"..."}],
              "ally":  [/* 4 entries, EXCLUDING the user's role */]
            }
            Use only champion names that exist in League of Legends. Do not include the user's role in the ally list.
            """;

    private final OpenAiService ai;
    private final DataDragonService dataDragon;
    private final RateLimit rateLimit;

    public DraftController(OpenAiService ai, DataDragonService dataDragon, RateLimit rateLimit) {
        this.ai = ai;
        this.dataDragon = dataDragon;
        this.rateLimit = rateLimit;
    }

    /** POST /api/v1/draft   body: {"role":"JGL"} */
    @PostMapping
    public DraftResponse generate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        rateLimit.consumeOrThrow(request);

        String userRole = Optional.ofNullable(body.get("role")).map(String::toUpperCase).orElse("");
        if (!ROLES.contains(userRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role must be one of " + ROLES);
        }

        String userPrompt = "User role: " + userRole + ". Generate the draft now.";
        String content = ai.chat(SYSTEM_MESSAGE, userPrompt, "draft");

        try {
            JsonNode root = parseJson(content);
            List<DraftPick> enemy = parseTeam(root.get("enemy"), null);
            List<DraftPick> ally = parseTeam(root.get("ally"), userRole);
            backfillIfShort(enemy, null, /*size*/ 5, ally);
            backfillIfShort(ally, userRole, /*size*/ 4, enemy);
            return new DraftResponse(userRole, enemy, ally);
        } catch (Exception e) {
            logger.error("Could not parse draft JSON: {}", content, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please refresh.");
        }
    }

    /** Strip ``` fences if the model added them anyway, then parse JSON. */
    private JsonNode parseJson(String content) throws Exception {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("(?s)```(json)?", "").trim();
        }
        return new ObjectMapper().readTree(cleaned);
    }

    private List<DraftPick> parseTeam(JsonNode arr, String excludedRole) {
        List<DraftPick> result = new ArrayList<>();
        if (arr == null || !arr.isArray()) return result;
        Set<String> seenRoles = new HashSet<>();
        for (JsonNode n : arr) {
            String role = n.path("role").asText("").toUpperCase(Locale.ROOT);
            String champ = n.path("champion").asText("");
            if (!ROLES.contains(role) || (excludedRole != null && role.equals(excludedRole))) continue;
            if (seenRoles.contains(role)) continue;
            Optional<Champion> c = dataDragon.byName(champ);
            if (c.isEmpty()) continue;
            seenRoles.add(role);
            result.add(new DraftPick(role, c.get().getId(), c.get().getName(), c.get().getIconUrl()));
        }
        return result;
    }

    /** If the AI dropped or hallucinated picks, fill missing roles from random unused champions. */
    private void backfillIfShort(List<DraftPick> team, String excludedRole, int targetSize, List<DraftPick> otherTeam) {
        if (team.size() >= targetSize) return;
        Set<String> filledRoles = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        for (DraftPick p : team) { filledRoles.add(p.getRole()); usedNames.add(p.getChampionName()); }
        for (DraftPick p : otherTeam) usedNames.add(p.getChampionName());

        List<Champion> pool = new ArrayList<>(dataDragon.all());
        Collections.shuffle(pool);

        for (String role : ROLES) {
            if (excludedRole != null && role.equals(excludedRole)) continue;
            if (filledRoles.contains(role)) continue;
            for (Champion c : pool) {
                if (!usedNames.contains(c.getName())) {
                    team.add(new DraftPick(role, c.getId(), c.getName(), c.getIconUrl()));
                    usedNames.add(c.getName());
                    break;
                }
            }
        }
        // Sort by canonical role order so the UI renders consistently.
        team.sort(Comparator.comparingInt(p -> ROLES.indexOf(p.getRole())));
    }
}
