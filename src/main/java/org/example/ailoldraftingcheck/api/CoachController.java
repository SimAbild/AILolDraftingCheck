package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.service.CoachService;
import org.example.ailoldraftingcheck.dtos.CoachRequest;
import org.example.ailoldraftingcheck.dtos.CoachResponse;
import org.springframework.web.bind.annotation.*;

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
