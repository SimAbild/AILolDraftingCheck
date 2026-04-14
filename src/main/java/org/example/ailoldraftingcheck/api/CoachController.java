package org.example.ailoldraftingcheck.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Reviews the user's champion pick and returns positives, negatives,
 * and up to 3 alternative champions with short reasons.
 *
 * Same IP-rate-limit pattern as DraftController and the example
 * JokeLimitedController.
 */
@RestController
@RequestMapping("/api/v1/coach")
@CrossOrigin(origins = "*")
public class CoachController {

    final static String SYSTEM_MESSAGE =
            "You are a League of Legends drafting coach." +
            " You will receive the user's role + chosen champion, their 4 ally picks, and the 5 enemy picks." +
            " Use champion knowledge about synergies with the ally team and counters to the enemy team." +
            " Reply with STRICT JSON only, no markdown, in this exact shape:" +
            " {\"positives\":[\"short sentence 1\",\"short sentence 2\",\"short sentence 3\"]," +
            "  \"negatives\":[\"short sentence 1\",\"short sentence 2\",\"short sentence 3\"]," +
            "  \"alternatives\":[" +
            "    {\"champion\":\"Name\",\"reason\":\"one short sentence\"}," +
            "    {\"champion\":\"Name\",\"reason\":\"...\"}," +
            "    {\"champion\":\"Name\",\"reason\":\"...\"}]}" +
            " Keep each bullet short. Alternative champions must be real League champions that fit the user's role.";

    @Value("${app.bucket_capacity}")
    private int BUCKET_CAPACITY;

    @Value("${app.refill_amount}")
    private int REFILL_AMOUNT;

    @Value("${app.refill_time}")
    private int REFILL_TIME;

    private final OpenAiService service;
    private final DataDragonService dataDragon;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public CoachController(OpenAiService service, DataDragonService dataDragon) {
        this.service = service;
        this.dataDragon = dataDragon;
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(BUCKET_CAPACITY, Refill.greedy(REFILL_AMOUNT, Duration.ofMinutes(REFILL_TIME)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createNewBucket());
    }

    @PostMapping
    public CoachResponse getCoach(@RequestBody CoachRequest req, HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        Bucket bucket = getBucket(ip);
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later");
        }

        if (req.getUserChampion() == null || req.getUserRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userChampion and userRole are required");
        }

        String userPrompt = buildUserPrompt(req);
        String content = service.chat(SYSTEM_MESSAGE, userPrompt, "coach");

        try {
            JsonNode root = parseJson(content);
            List<String> positives = toStrings(root.get("positives"));
            List<String> negatives = toStrings(root.get("negatives"));
            List<CoachResponse.Alternative> alts = parseAlternatives(root.get("alternatives"));
            return new CoachResponse(positives, negatives, alts);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private String buildUserPrompt(CoachRequest req) {
        String ally = teamToLine(req.getAllyTeam());
        String enemy = teamToLine(req.getEnemyTeam());
        return "user_role: " + req.getUserRole() +
               "\nuser_champion: " + req.getUserChampion() +
               "\nally_team (4): " + ally +
               "\nenemy_team (5): " + enemy;
    }

    private String teamToLine(List<DraftPick> team) {
        if (team == null || team.isEmpty()) return "(none)";
        return team.stream()
                .map(p -> p.getRole() + ":" + p.getChampionName())
                .collect(Collectors.joining(", "));
    }

    private JsonNode parseJson(String content) throws Exception {
        String clean = content.trim();
        if (clean.startsWith("```")) clean = clean.replaceAll("(?s)```(json)?", "").trim();
        return new ObjectMapper().readTree(clean);
    }

    private List<String> toStrings(JsonNode arr) {
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
