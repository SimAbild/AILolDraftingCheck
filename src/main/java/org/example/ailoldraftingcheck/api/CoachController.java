package org.example.ailoldraftingcheck.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpGgScraperService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trin 4 in the flow: the user has picked a champion, now the AI explains
 * what's good, what's bad, and suggests up to 3 alternatives.
 *
 * Pipeline:
 *   1. Look up best-effort counter data from op.gg (may be empty).
 *   2. Build a JSON-only prompt that includes the full draft + counter hints.
 *   3. Ask ChatGPT.
 *   4. Parse, attach icons via Data Dragon, return.
 */
@RestController
@RequestMapping("/api/v1/coach")
@CrossOrigin(origins = "*")
public class CoachController {

    private static final Logger logger = LoggerFactory.getLogger(CoachController.class);

    private static final String SYSTEM_MESSAGE = """
            You are a League of Legends drafting coach.
            You will receive: the user's chosen champion + role, their 4 ally picks, and the 5 enemy picks.
            You may also receive 'counter_hints' - champions that op.gg lists as counters to the user's pick.
            You need to use champion knowledge in regards to synergies with ally team and counters for enemy team.
            Use them as a tie-breaker but rely on champion knowledge for the actual reasoning.
            

            Reply with STRICT JSON ONLY, no markdown, in this exact shape:
            {
              "positives": ["short bullet 1", "short bullet 2", "short bullet 3"],
              "negatives": ["short bullet 1", "short bullet 2", "short bullet 3"],
              "alternatives": [
                {"champion":"Name", "reason":"one short sentence why this is a stronger pick"},
                {"champion":"Name", "reason":"..."},
                {"champion":"Name", "reason":"..."}
              ]
            }
            Each bullet must be one short sentence, written for a learning player.
            Alternative champions must be real League champions and must fit the user's role.
            """;

    private final OpenAiService ai;
    private final DataDragonService dataDragon;
    private final OpGgScraperService opgg;
    private final RateLimit rateLimit;

    public CoachController(OpenAiService ai, DataDragonService dataDragon,
                           OpGgScraperService opgg, RateLimit rateLimit) {
        this.ai = ai;
        this.dataDragon = dataDragon;
        this.opgg = opgg;
        this.rateLimit = rateLimit;
    }

    /** POST /api/v1/coach   body: CoachRequest */
    @PostMapping
    public CoachResponse coach(@RequestBody CoachRequest req, HttpServletRequest request) {
        rateLimit.consumeOrThrow(request);

        if (req.getUserChampion() == null || req.getUserRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userChampion and userRole are required");
        }

        // Best-effort live data
        List<String> counterHints = opgg.fetchCounters(req.getUserChampion(), req.getUserRole());
        String dataSource = counterHints.isEmpty() ? "llm-only" : "op.gg-scrape";

        String userPrompt = buildUserPrompt(req, counterHints);
        String content = ai.chat(SYSTEM_MESSAGE, userPrompt, "coach");

        try {
            JsonNode root = parseJson(content);
            List<String> positives = toStringList(root.get("positives"));
            List<String> negatives = toStringList(root.get("negatives"));
            List<CoachResponse.Alternative> alts = parseAlternatives(root.get("alternatives"));
            return new CoachResponse(positives, negatives, alts, dataSource);
        } catch (Exception e) {
            logger.error("Could not parse coach JSON: {}", content, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private String buildUserPrompt(CoachRequest req, List<String> counterHints) {
        String allyStr = req.getAllyTeam() == null ? "(none)" :
                req.getAllyTeam().stream()
                        .map(p -> p.getRole() + ":" + p.getChampionName())
                        .collect(Collectors.joining(", "));
        String enemyStr = req.getEnemyTeam() == null ? "(none)" :
                req.getEnemyTeam().stream()
                        .map(p -> p.getRole() + ":" + p.getChampionName())
                        .collect(Collectors.joining(", "));
        return """
                user_role: %s
                user_champion: %s
                ally_team (4): %s
                enemy_team (5): %s
                counter_hints: %s
                """.formatted(req.getUserRole(), req.getUserChampion(), allyStr, enemyStr,
                              counterHints.isEmpty() ? "(none)" : String.join(", ", counterHints));
    }

    private JsonNode parseJson(String content) throws Exception {
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("(?s)```(json)?", "").trim();
        }
        return new ObjectMapper().readTree(cleaned);
    }

    private List<String> toStringList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) return out;
        for (JsonNode n : arr) out.add(n.asText());
        return out;
    }

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
