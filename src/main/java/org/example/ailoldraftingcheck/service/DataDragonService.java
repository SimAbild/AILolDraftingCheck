package org.example.ailoldraftingcheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the official Riot champion list (id, display name, square icon URL)
 * from Data Dragon. Public, free, no API key required.
 *
 * The patch version is fetched once at startup; if that fails we fall back to
 * {@code app.datadragon.fallback-version}. Champions are cached in memory for
 * the rest of the app's lifetime so we don't re-download on every request.
 *
 * Visual analogy: think of Data Dragon as League's "phonebook" - one big JSON
 * file with every champion's name and image, refreshed each patch.
 */
@Service
public class DataDragonService {

    private static final Logger logger = LoggerFactory.getLogger(DataDragonService.class);

    @Value("${app.datadragon.fallback-version}")
    private String fallbackVersion;

    private final WebClient client = WebClient.create();
    private final Map<String, Champion> championsById = new ConcurrentHashMap<>();
    private final Map<String, Champion> championsByLowerName = new ConcurrentHashMap<>();
    private String version;

    @PostConstruct
    public void load() {
        try {
            this.version = fetchLatestVersion();
            logger.info("Data Dragon: using patch {}", version);
            loadChampions(version);
        } catch (Exception e) {
            logger.warn("Data Dragon load failed ({}), retrying with fallback {}", e.getMessage(), fallbackVersion);
            try {
                this.version = fallbackVersion;
                loadChampions(fallbackVersion);
            } catch (Exception inner) {
                logger.error("Data Dragon fallback also failed - champion list will be empty until restart", inner);
            }
        }
    }

    private String fetchLatestVersion() {
        // versions.json is a JSON array, newest first. e.g. ["14.7.1","14.6.1",...]
        String body = client.get()
                .uri("https://ddragon.leagueoflegends.com/api/versions.json")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode arr = new ObjectMapper().readTree(body);
            return arr.get(0).asText();
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse versions.json", e);
        }
    }

    private void loadChampions(String patch) throws Exception {
        String url = "https://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/champion.json";
        String body = client.get().uri(url).retrieve().bodyToMono(String.class).block();
        JsonNode root = new ObjectMapper().readTree(body);
        JsonNode data = root.get("data");

        Iterator<Map.Entry<String, JsonNode>> it = data.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String id = e.getKey();                            // e.g. "MonkeyKing"
            String name = e.getValue().get("name").asText();   // e.g. "Wukong"
            String iconUrl = "https://ddragon.leagueoflegends.com/cdn/" + patch + "/img/champion/" + id + ".png";
            Champion c = new Champion(id, name, iconUrl);
            championsById.put(id, c);
            championsByLowerName.put(name.toLowerCase(Locale.ROOT), c);
        }
        logger.info("Data Dragon: loaded {} champions", championsById.size());
    }

    /** Full champion list for the autocomplete picker. */
    public List<Champion> all() {
        List<Champion> list = new ArrayList<>(championsById.values());
        list.sort(Comparator.comparing(Champion::getName));
        return list;
    }

    public Optional<Champion> byName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(championsByLowerName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<Champion> byId(String id) {
        return Optional.ofNullable(championsById.get(id));
    }

    public String getVersion() {
        return version;
    }
}
