package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.DraftPickRequest;
import org.example.ailoldraftingcheck.service.DraftService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.example.ailoldraftingcheck.dtos.Champion;
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

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends draft generator." +
                    " Given the user's role, output a realistic draft for the OTHER nine slots:" +
                    " 5 enemy champions and 4 ally champions (exclude the user's role from the ally list)." +
                    " Reply with STRICT JSON only, no markdown, in this exact shape:" +
                    " {\"enemy\":[{\"role\":\"TOP\",\"champion\":\"Aatrox\"}, ...5 entries...]," +
                    " \"ally\":[{\"role\":\"JGL\",\"champion\":\"...\"}, ...4 entries, EXCLUDING user role...]}" +
                    " Use champion names that exist in League of Legends.";

    private final OpenAiService openAiService;
    private final DraftService draftService;

    public DraftController(OpenAiService openAiService, DraftService draftService) {
        this.openAiService = openAiService;
        this.draftService = draftService;
    }

    @PostMapping
    public DraftResponse generateDraft(@RequestBody Map<String, String> requestBody) {
        String userRole = draftService.extractAndValidateRole(requestBody);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, "User role: " + userRole);

        try {
            JsonNode responseJson = openAiService.parseAiReply(aiReply);
            List<DraftPickRequest> enemyTeam = draftService.buildTeamPicks(responseJson.get("enemy"), null);
            List<DraftPickRequest> allyTeam = draftService.buildTeamPicks(responseJson.get("ally"), userRole);
            return new DraftResponse(userRole, enemyTeam, allyTeam);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }
}
