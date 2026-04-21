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

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping
    public CoachResponse analyzeChampionPick(@RequestBody CoachRequest coachRequest) {
        return coachService.analyzeChampionPick(coachRequest);
    }
}
