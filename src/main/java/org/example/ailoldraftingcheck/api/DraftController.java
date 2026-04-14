package org.example.ailoldraftingcheck.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This controller asks ChatGPT to generate a realistic draft around the user's
 * role: 5 enemy picks + 4 ally picks (the user's role slot is left for them
 * to fill themselves).
 *
 * The request is IP-rate limited using bucket4j, same pattern as the
 * JokeLimitedController in the chatgpt-jokes example.
 */
@RestController
@RequestMapping("/api/v1/draft")
@CrossOrigin(origins = "*")
public class DraftController {

    private static final List<String> ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    final static String SYSTEM_MESSAGE =
            "You are a League of Legends draft generator." +
            " Given the user's role, output a realistic draft for the OTHER nine slots:" +
            " 5 enemy champions and 4 ally champions (exclude the user's role from the ally list)." +
            " Reply with STRICT JSON only, no markdown, in this exact shape:" +
            " {\"enemy\":[{\"role\":\"TOP\",\"champion\":\"Aatrox\"}, ...5 entries...]," +
            "  \"ally\":[{\"role\":\"JGL\",\"champion\":\"...\"}, ...4 entries, EXCLUDING user role...]}" +
            " Use champion names that exist in League of Legends.";

    @Value("${app.bucket_capacity}")
    private int BUCKET_CAPACITY;

    @Value("${app.refill_amount}")
    private int REFILL_AMOUNT;

    @Value("${app.refill_time}")
    private int REFILL_TIME;

    private final OpenAiService service;
    private final DataDragonService dataDragon;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public DraftController(OpenAiService service, DataDragonService dataDragon) {
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
    public DraftResponse getDraft(@RequestBody Map<String, String> body, HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        Bucket bucket = getBucket(ip);
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later");
        }

        String userRole = Optional.ofNullable(body.get("role"))
                .map(String::toUpperCase).orElse("");
        if (!ROLES.contains(userRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be one of " + ROLES);
        }

        String content = service.chat(SYSTEM_MESSAGE, "User role: " + userRole, "draft");

        try {
            JsonNode root = parseJson(content);
            List<DraftPick> enemy = parseTeam(root.get("enemy"), null);
            List<DraftPick> ally = parseTeam(root.get("ally"), userRole);
            return new DraftResponse(userRole, enemy, ally);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private JsonNode parseJson(String content) throws Exception {
        String clean = content.trim();
        if (clean.startsWith("```")) clean = clean.replaceAll("(?s)```(json)?", "").trim();
        return new ObjectMapper().readTree(clean);
    }

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
