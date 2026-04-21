package org.example.ailoldraftingcheck.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class DataDragonService {

    private static final Logger logger = LoggerFactory.getLogger(DataDragonService.class);
    private static final String DATA_DRAGON_BASE_URL = "https://ddragon.leagueoflegends.com";
    private static final String VERSIONS_URL = DATA_DRAGON_BASE_URL + "/api/versions.json";
    private static final int NEWEST_PATCH_INDEX = 0;


    private final WebClient webClient = WebClient.builder().build();

    private final ObjectMapper jsonMapper = JsonMapper.builder().build();

    private final List<Champion> champions = new ArrayList<>();
    private final Map<String, Champion> championsByLowercaseName = new HashMap<>();

    @PostConstruct
    public void load() {
        try {
            String latestPatch = fetchLatestPatch();
            loadChampions(latestPatch);
        } catch (Exception e) {
            logger.warn("Data Dragon failed ({})", e.getMessage());
        }
    }

    private String fetchLatestPatch() throws Exception {
        String versionsJson = webClient.get()
                .uri(VERSIONS_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return jsonMapper.readTree(versionsJson).get(NEWEST_PATCH_INDEX).asText();
    }

    private void loadChampions(String patch) throws Exception {
        String championsJson = fetchChampionData(patch);
        JsonNode championDataNode = jsonMapper.readTree(championsJson).get("data");

        for (Map.Entry<String, JsonNode> entry : championDataNode.properties()) {
            Champion champion = parseChampion(patch, entry);
            champions.add(champion);
            championsByLowercaseName.put(champion.getName().toLowerCase(Locale.ROOT), champion);
        }

        champions.sort(Comparator.comparing(Champion::getName));
        logger.info("Data Dragon: loaded {} champions (patch {})", champions.size(), patch);
    }

    private String fetchChampionData(String patch) {
        String championDataUrl = DATA_DRAGON_BASE_URL + "/cdn/" + patch + "/data/en_US/champion.json";
        return webClient.get()
                .uri(championDataUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Champion parseChampion(String patch, Map.Entry<String, JsonNode> entry) {
        String championId = entry.getKey();
        String championName = entry.getValue().get("name").asText();
        String iconUrl = DATA_DRAGON_BASE_URL + "/cdn/" + patch + "/img/champion/" + championId + ".png";
        return new Champion(championId, championName, iconUrl);
    }

    public List<Champion> getAllChampions() {
        return List.copyOf(champions);
    }

    public Optional<Champion> findChampionByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(championsByLowercaseName.get(name.toLowerCase(Locale.ROOT)));
    }
}
