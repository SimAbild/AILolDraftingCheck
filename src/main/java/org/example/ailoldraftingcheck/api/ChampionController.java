package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.Champion;
import org.example.ailoldraftingcheck.service.DataDragonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/champions")
@CrossOrigin(origins = "*")
public class ChampionController {

    private final DataDragonService dataDragonService;

    public ChampionController(DataDragonService dataDragonService) {
        this.dataDragonService = dataDragonService;
    }

    @GetMapping
    public List<Champion> getChampions() {
        return dataDragonService.getAllChampions();
    }
}
