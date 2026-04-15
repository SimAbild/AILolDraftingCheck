package org.example.ailoldraftingcheck.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Loads the official Riot champion list from Data Dragon once at startup.
 *
 * Data Dragon is Riot's free public "phonebook" of champions: one JSON file
 * per patch with every champion's name and square icon URL.
 *
 * We use the same WebClient pattern as OpenAiService.
 */
@Service
public class DataDragonService {

    public static final Logger logger = LoggerFactory.getLogger(DataDragonService.class);
    private static final String FALLBACK_PATCH = "14.7.1";

    private final WebClient client = WebClient.builder().build();
    // Jackson 3: use JsonMapper.builder().build() instead of new ObjectMapper().
    private final ObjectMapper mapper = JsonMapper.builder().build();

    // Champion data we loaded. Kept in memory for the life of the app.
    private final List<Champion> champions = new ArrayList<>();
    private final Map<String, Champion> byLowerName = new HashMap<>();

    /**
     * Runs once after Spring has built the bean. If something goes wrong we fall
     * back to a known patch so the app still starts with a champion list.
     */
    @PostConstruct
    public void load() {
        try {
            String patch = fetchLatestPatch();
            loadChampions(patch);
        } catch (Exception e) {
            logger.warn("Data Dragon failed (" + e.getMessage() + "), using fallback patch " + FALLBACK_PATCH);
            try {
                loadChampions(FALLBACK_PATCH);
            } catch (Exception inner) {
                logger.error("Data Dragon fallback also failed", inner);
            }
        }
    }

    /** Ask Data Dragon which patch is current. versions.json is an array, newest first. */
    private String fetchLatestPatch() throws Exception {
        String body = client.get()
                .uri("https://ddragon.leagueoflegends.com/api/versions.json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return mapper.readTree(body).get(0).asText();
    }

    /** Download the champion list for the given patch and populate our maps. */
    private void loadChampions(String patch) throws Exception {
        String url = "https://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/champion.json";
        String body = client.get().uri(url).retrieve().bodyToMono(String.class).block();

        // "data" is an object: { "Aatrox": {...}, "Ahri": {...}, ... }
        // properties() gives us every (key, value) pair so we can read each champion.
        JsonNode data = mapper.readTree(body).get("data");
        for (Map.Entry<String, JsonNode> entry : data.properties()) {
            String id = entry.getKey();                            // e.g. "MonkeyKing"
            String name = entry.getValue().get("name").asText();   // e.g. "Wukong"
            String iconUrl = "https://ddragon.leagueoflegends.com/cdn/" + patch + "/img/champion/" + id + ".png";

            Champion c = new Champion(id, name, iconUrl);
            champions.add(c);
            byLowerName.put(name.toLowerCase(Locale.ROOT), c);
        }
        champions.sort(Comparator.comparing(Champion::getName));
        logger.info("Data Dragon: loaded " + champions.size() + " champions (patch " + patch + ")");
    }

    /** The full champion list used by the autocomplete picker. */
    public List<Champion> all() {
        return List.copyOf(champions);
    }

    /** Look up a champion by display name (case-insensitive). Empty if unknown. */
    public Optional<Champion> byName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byLowerName.get(name.toLowerCase(Locale.ROOT)));
    }
}
