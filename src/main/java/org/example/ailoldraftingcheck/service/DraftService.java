package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.dtos.DraftPickRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.*;

@Service
public class DraftService {

    DataDragonService dataDragonService;

    private static final List<String> VALID_ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");

    public DraftService(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
    }

    public String extractAndValidateRole(Map<String, String> requestBody) {
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

    public List<DraftPickRequest> buildTeamPicks(JsonNode jsonArray, String excludedRole) {
        List<DraftPickRequest> picks = new ArrayList<>();

        if (jsonArray == null || !jsonArray.isArray()) return picks;

        for (JsonNode pickNode : jsonArray) {
            String role = pickNode.path("role").asText("").toUpperCase(Locale.ROOT);
            String championName = pickNode.path("champion").asText("");

            if (shouldSkipPick(role, excludedRole)) continue;

            Optional<Champion> maybeChampion = dataDragonService.findChampionByName(championName);
            if (maybeChampion.isEmpty()) continue;

            picks.add(new DraftPickRequest(role, maybeChampion.get().getName(), maybeChampion.get().getIconUrl()));
        }
        return picks;
    }

    public boolean shouldSkipPick(String role, String excludedRole) {
        boolean isUnknownRole = !VALID_ROLES.contains(role);
        boolean isExcludedRole = excludedRole != null && role.equals(excludedRole);
        return isUnknownRole || isExcludedRole;
    }

}
