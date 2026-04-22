package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.AiDraft;
import org.example.ailoldraftingcheck.dtos.AiDraftChampionEntry;
import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftPick;
import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class DraftService {

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

    public DraftService(OpenAiService openAiService, DataDragonService dataDragonService) {
        this.openAiService = openAiService;
        this.dataDragonService = dataDragonService;
    }

    public DraftResponse generateDraft(String rawRole) {
        String userRole = rawRole.toUpperCase();
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        try {
            // Jackson deserialiserer JSON-svaret direkte til AiDraft
            // uden at vi behøver at navigere et JSON-træ manuelt.
            AiDraft aiDraft = openAiService.parseJsonReply(aiReply, AiDraft.class);
            List<DraftPick> enemyTeam = resolveTeamPicks(aiDraft.getEnemy(), null);
            List<DraftPick> allyTeam = resolveTeamPicks(aiDraft.getAlly(), userRole);
            return new DraftResponse(userRole, enemyTeam, allyTeam);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private List<DraftPick> resolveTeamPicks(List<AiDraftChampionEntry> aiPicks, String excludedRole) {
        if (aiPicks == null) return List.of();

        List<DraftPick> picks = new ArrayList<>();
        for (AiDraftChampionEntry aiPick : aiPicks) {
            String role = normalizeRole(aiPick.getRole().toUpperCase(Locale.ROOT));
            if (shouldSkipPick(role, excludedRole)) continue;

            // Optional bruges her fordi en champion måske ikke kendes i Data Dragon.
            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(aiPick.getChampion());
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
