package org.example.ailoldraftingcheck.service;

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
            " {\"enemy\":[{\"role\":\"TOP\",\"name\":\"Aatrox\"},{\"role\":\"JGL\",\"name\":\"Vi\"}," +
            "{\"role\":\"MID\",\"name\":\"Lux\"},{\"role\":\"ADC\",\"name\":\"Jinx\"},{\"role\":\"SUPP\",\"name\":\"Thresh\"}]," +
            " \"ally\":[{\"role\":\"TOP\",\"name\":\"...\"},{\"role\":\"JGL\",\"name\":\"...\"}," +
            "{\"role\":\"MID\",\"name\":\"...\"},{\"role\":\"ADC\",\"name\":\"...\"}]}" +
            " Roles MUST use EXACTLY these abbreviations: TOP, JGL, MID, ADC, SUPP." +
            " Use champion names that exist in League of Legends.";

    private final OpenAiService openAiService;

    public DraftService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public DraftResponse generateDraft(String rawRole) {
        String userRole = rawRole.toUpperCase();
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        try {
            DraftResponse draft = openAiService.parseJsonReply(aiReply, DraftResponse.class);
            draft.setEnemy(resolveTeamPicks(draft.getEnemy(), null));
            draft.setAlly(resolveTeamPicks(draft.getAlly(), userRole));
            return draft;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }

    private List<DraftPick> resolveTeamPicks(List<DraftPick> picks, String excludedRole) {
        if (picks == null) return List.of();

        List<DraftPick> resolvedPicks = new ArrayList<>();
        for (DraftPick pick : picks) {
            String role = normalizeRole(pick.getRole().toUpperCase(Locale.ROOT));
            if (shouldSkipPick(role, excludedRole)) continue;

            resolvedPicks.add(new DraftPick(role, pick.getName()));
        }
        return resolvedPicks;
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
