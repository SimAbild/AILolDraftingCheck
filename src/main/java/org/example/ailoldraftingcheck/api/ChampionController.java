package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only endpoint the frontend calls once so the autocomplete picker
 * knows every champion name + icon.
 */
@RestController
@RequestMapping("/api/v1/champions")
@CrossOrigin(origins = "*")
public class ChampionController {

    private final DataDragonService service;

    public ChampionController(DataDragonService service) {
        this.service = service;
    }

    /** GET /api/v1/champions  ->  the full champion list (sorted by name). */
    @GetMapping
    public List<Champion> getChampions() {
        return service.all();
    }
}
