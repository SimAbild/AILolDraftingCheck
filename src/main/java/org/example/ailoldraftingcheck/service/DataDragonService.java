package org.example.ailoldraftingcheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Loads the official Riot Data Dragon champion list at startup and keeps it
 * in memory. Uses the same WebClient + .block() pattern as OpenAiService.
 *
 * Data Dragon is Riot's free, public "phonebook" of champions - one JSON
 * file per patch with every champion's name and square icon.
 */
@Service
public class DataDragonService {

    public static final Logger logger = LoggerFactory.getLogger(DataDragonService.class);
    private static final String FALLBACK_PATCH = "14.7.1";

    private final WebClient client = WebClient.create();

    private final List<Champion> champions = new ArrayList<>();
    private final Map<String, Champion> byLowerName = new HashMap<>();

    @PostConstruct
    public void load() {
        try {
            String patch = fetchLatestPatch();
            loadChampions(patch);
        } catch (Exception e) {
            logger.warn("Data Dragon load failed (" + e.getMessage() + "), retrying with fallback " + FALLBACK_PATCH);
            try {
                loadChampions(FALLBACK_PATCH);
            } catch (Exception inner) {
                logger.error("Data Dragon fallback also failed: " + inner.getMessage());
            }
        }
    }

    private String fetchLatestPatch() throws Exception {
        String body = client.get()
                .uri("https://ddragon.leagueoflegends.com/api/versions.json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return new ObjectMapper().readTree(body).get(0).asText();
    }

    private void loadChampions(String patch) throws Exception {
        String url = "https://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/champion.json";
        String body = client.get().uri(url).retrieve().bodyToMono(String.class).block();
        JsonNode data = new ObjectMapper().readTree(body).get("data");

        Iterator<Map.Entry<String, JsonNode>> it = data.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String id = entry.getKey();
            String name = entry.getValue().get("name").asText();
            String iconUrl = "https://ddragon.leagueoflegends.com/cdn/" + patch + "/img/champion/" + id + ".png";
            Champion c = new Champion(id, name, iconUrl);
            champions.add(c);
            byLowerName.put(name.toLowerCase(Locale.ROOT), c);
        }
        champions.sort(Comparator.comparing(Champion::getName));
        logger.info("Data Dragon: loaded " + champions.size() + " champions (patch " + patch + ")");
    }

    public List<Champion> all() {
        return List.copyOf(champions);
    }

    public Optional<Champion> byName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byLowerName.get(name.toLowerCase(Locale.ROOT)));
    }
}
