package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.example.ailoldraftingcheck.service.DraftService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/draft")
@CrossOrigin(origins = "*")
public class DraftController {

    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    public DraftResponse generateDraft(@RequestBody Map<String, String> requestBody) {
        return draftService.generateDraft(requestBody.get("role"));
    }
}
