package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only endpoint the frontend hits once on the draft page so the
 * autocomplete picker can search by name. No rate limit - this is a static
 * list served from memory.
 */
@RestController
@RequestMapping("/api/v1/champions")
@CrossOrigin(origins = "*")
public class ChampionController {

    private final DataDragonService dataDragon;

    public ChampionController(DataDragonService dataDragon) {
        this.dataDragon = dataDragon;
    }

    /** GET /api/v1/champions  ->  full champion list, sorted by name. */
    @GetMapping
    public List<Champion> all() {
        return dataDragon.all();
    }

    /** GET /api/v1/champions/version  ->  the Data Dragon patch we're using. */
    @GetMapping("/version")
    public String version() {
        return dataDragon.getVersion();
    }
}
