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

@RestController
@RequestMapping("/api/v1/draft")
@CrossOrigin(origins = "*")
public class DraftController {

    private static final List<String> VALID_ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends draft generator." +
            " Given the user's role, output a realistic draft for the OTHER nine slots:" +
            " 5 enemy champions and 4 ally champions (exclude the user's role from the ally list)." +
            " Reply with STRICT JSON only, no markdown, in this exact shape:" +
            " {\"enemy\":[{\"role\":\"TOP\",\"champion\":\"Aatrox\"},{\"role\":\"JGL\",\"champion\":\"Vi\"}," +
            "{\"role\":\"MID\",\"champion\":\"Lux\"},{\"role\":\"ADC\",\"champion\":\"Jinx\"},{\"role\":\"SUPP\",\"champion\":\"Thresh\"}]," +
            " \"ally\":[{\"role\":\"TOP\",\"champion\":\"...\"},{\"role\":\"JGL\",\"champion\":\"...\"}," +
            "{\"role\":\"MID\",\"champion\":\"...\"},{\"role\":\"ADC\",\"champion\":\"...\"}]}" +
            " Roles MUST use EXACTLY these abbreviations: TOP, JGL, MID, ADC, SUPP." +
            " Use champion names that exist in League of Legends.";

    private final OpenAiService openAiService;
    private final DataDragonService dataDragonService;

    public DraftController(OpenAiService openAiService, DataDragonService dataDragonService) {
        this.openAiService = openAiService;
        this.dataDragonService = dataDragonService;
    }

    @PostMapping
    public DraftResponse generateDraft(@RequestBody Map<String, String> requestBody) {
        String userRole = extractAndValidateRole(requestBody);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        try {
            // JsonNode er Jacksons "træ-model" til at navigere JSON dynamisk,
            // uden at skulle deserialisere hele svaret til en fast Java-klasse.
            // Vi bruger det her fordi AI-svarets JSON-struktur er kompleks
            // og indeholder nestede arrays med objekter.
            JsonNode responseJson = parseAiReply(aiReply);
            List<DraftPick> enemyTeam = buildTeamPicks(responseJson.get("enemy"), null);
            List<DraftPick> allyTeam = buildTeamPicks(responseJson.get("ally"), userRole);
            return new DraftResponse(userRole, enemyTeam, allyTeam);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private String extractAndValidateRole(Map<String, String> requestBody) {
        String userRole = Optional.ofNullable(requestBody.get("role"))
                .map(String::toUpperCase)
                .orElse("");
        boolean isValidRole = VALID_ROLES.contains(userRole);
        if (!isValidRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "role must be one of " + VALID_ROLES);
        }
        return userRole;
    }

    private JsonNode parseAiReply(String content) throws Exception {
        String cleanContent = content.trim();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.replaceAll("(?s)```(json)?", "").trim();
        }
        return JsonMapper.builder().build().readTree(cleanContent);
    }

    private List<DraftPick> buildTeamPicks(JsonNode jsonArray, String excludedRole) {
        List<DraftPick> picks = new ArrayList<>();

        // .isArray() tjekker om JsonNode-en indeholder et JSON-array ([ ... ]).
        // Det er en sikkerhedstjek — AI'en kan i sjældne tilfælde svare forkert.
        if (jsonArray == null || !jsonArray.isArray()) return picks;

        for (JsonNode pickNode : jsonArray) {
            // .path("role") læser feltet "role" fra JSON-objektet.
            // Forskellen på .get() og .path() er at .path() returnerer en
            // tom node (i stedet for null) hvis feltet ikke findes — det undgår NullPointerException.
            // .asText("") konverterer nodeværdien til String, med "" som fallback.
            String role = normalizeRole(pickNode.path("role").asText("").toUpperCase(Locale.ROOT));
            String championName = pickNode.path("champion").asText("");

            if (shouldSkipPick(role, excludedRole)) continue;

            // Optional bruges her fordi en champion måske ikke kendes i Data Dragon.
            // .isEmpty() tjekker om Optional er tom — altså om champion ikke blev fundet.
            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(championName);
            if (maybeChampion.isEmpty()) continue;

            picks.add(new DraftPick(role, maybeChampion.get().getName(), maybeChampion.get().getIconUrl()));
        }
        return picks;
    }

    private String normalizeRole(String role) {
        return switch (role) {
            case "SUPPORT", "SUP" -> "SUPP";
            case "JUNGLE", "JUNGLER" -> "JGL";
            case "MIDDLE" -> "MID";
            case "BOTTOM", "BOT" -> "ADC";
            default -> role;
        };
    }

    private boolean shouldSkipPick(String role, String excludedRole) {
        boolean isUnknownRole = !VALID_ROLES.contains(role);
        boolean isExcludedRole = excludedRole != null && role.equals(excludedRole);
        return isUnknownRole || isExcludedRole;
    }
}
