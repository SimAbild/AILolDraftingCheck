package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DraftResponse {

    @JsonProperty("enemy")
    private List<DraftPick> enemy;

    @JsonProperty("ally")
    private List<DraftPick> ally;
}
