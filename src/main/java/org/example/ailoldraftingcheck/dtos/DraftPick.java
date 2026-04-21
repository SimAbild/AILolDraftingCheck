package org.example.ailoldraftingcheck.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Repræsenterer ét champion-valg i et LoL-draft med rolle, navn og ikon.
// Oprettes i kode (kræver @AllArgsConstructor) og deserialiseres fra frontend
// via CoachRequest (kræver @NoArgsConstructor til Jackson).
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DraftPick {
    private String role;
    private String championName;
    private String iconUrl;
}
