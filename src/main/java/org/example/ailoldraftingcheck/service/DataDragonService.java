package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.DataDragonResponse;
import tools.jackson.core.type.TypeReference;
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
    private static final String FALLBACK_PATCH = "16.8.1";
    private static final String DATA_DRAGON_BASE_URL = "https://ddragon.leagueoflegends.com";
    private static final String VERSIONS_URL = DATA_DRAGON_BASE_URL + "/api/versions.json";
    private static final int NEWEST_PATCH_INDEX = 0;

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper jsonMapper = JsonMapper.builder().build();

    private final List<Champion> champions = new ArrayList<>();



    @PostConstruct
    public void load() {
        try {
            String latestPatch = fetchLatestPatch();
            loadChampions(latestPatch);
        } catch (Exception e) {
            logger.warn("Data Dragon failed ({}), using fallback patch {}", e.getMessage(), FALLBACK_PATCH);
            try {
                loadChampions(FALLBACK_PATCH);
            } catch (Exception fallbackException) {
                logger.error("Data Dragon fallback also failed", fallbackException);
            }
        }
    }

    private String fetchLatestPatch() throws Exception {
        String versionsJson = webClient.get()
                .uri(VERSIONS_URL)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        List<String> versions = jsonMapper.readValue(versionsJson, new TypeReference<>() {});
        return versions.get(NEWEST_PATCH_INDEX);
    }

    private void loadChampions(String patch) throws Exception {
        String championsJson = fetchChampionData(patch);
        DataDragonResponse response = jsonMapper.readValue(championsJson, DataDragonResponse.class);

        for (Champion champion : response.getData().values()) {
            champion.setIconUrl(DATA_DRAGON_BASE_URL + "/cdn/" + patch + "/img/champion/" + champion.getId() + ".png");
            champions.add(champion);
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

    public List<Champion> getAllChampions() {
        return List.copyOf(champions);
    }

}
