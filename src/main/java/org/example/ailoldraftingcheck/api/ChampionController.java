package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only list of champions for the autocomplete picker. Served from memory,
 * so no rate-limit needed.
 */
@RestController
@RequestMapping("/api/v1/champions")
@CrossOrigin(origins = "*")
public class ChampionController {

    private final DataDragonService service;

    public ChampionController(DataDragonService service) {
        this.service = service;
    }

    @GetMapping
    public List<Champion> getChampions() {
        return service.all();
    }
}
