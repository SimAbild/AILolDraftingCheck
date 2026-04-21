package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.service.CoachService;
import tools.jackson.databind.JsonNode;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.example.ailoldraftingcheck.service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/v1/coach")
@CrossOrigin(origins = "*")
public class CoachController {

    private static final String SYSTEM_MESSAGE =
            "You are a League of Legends drafting coach." +
                    " You will receive the user's role + chosen champion, their 4 ally picks, and the 5 enemy picks." +
                    " Use champion knowledge about synergies with the ally team and counters to the enemy team." +
                    " Reply with STRICT JSON only, no markdown, in this exact shape:" +
                    " {\"positives\":[\"short sentence 1\",\"short sentence 2\",\"short sentence 3\"]," +
                    " \"negatives\":[\"short sentence 1\",\"short sentence 2\",\"short sentence 3\"]," +
                    " \"alternatives\":[" +
                    " {\"champion\":\"Name\",\"reason\":\"one short sentence\"}," +
                    " {\"champion\":\"Name\",\"reason\":\"...\"}," +
                    " {\"champion\":\"Name\",\"reason\":\"...\"}]}" +
                    " Keep each bullet short. Alternatives must be real League champions that fit the user's role.";

    private final OpenAiService openAiService;
    private final CoachService coachService;

    public CoachController(OpenAiService openAiService, CoachService coachService) {
        this.openAiService = openAiService;
        this.coachService = coachService;
    }

    @PostMapping
    public CoachResponse analyzeChampionPick(@RequestBody CoachRequest coachRequest) {
        if (coachService.isMissingRequiredFields(coachRequest)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userChampion and userRole are required");
        }

        String userPrompt = coachService.buildUserPrompt(coachRequest);
        String aiReply = openAiService.chat(SYSTEM_MESSAGE, userPrompt);

        try {
            JsonNode responseJson = coachService.parseAiReply(aiReply);
            List<String> positives = coachService.extractStringList(responseJson.get("positives"));
            List<String> negatives = coachService.extractStringList(responseJson.get("negatives"));
            List<CoachResponse.Alternative> alternatives = coachService.parseChampionAlternatives(responseJson.get("alternatives"));
            return new CoachResponse(positives, negatives, alternatives);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI returned invalid JSON. Please try again.");
        }
    }
}
