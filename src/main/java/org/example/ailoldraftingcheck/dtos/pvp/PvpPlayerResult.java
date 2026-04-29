package org.example.ailoldraftingcheck.dtos.pvp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.ailoldraftingcheck.dtos.CoachResponse;

// Et samlet resultat for én spiller — bruges i både reveal og results.
// Ved reveal: points=0 og feedback=null (endnu ikke beregnet).
// Ved results: alle felter udfyldt.
@Getter
@Setter
@AllArgsConstructor
public class PvpPlayerResult {
    private String username;
    private String champion;  // null hvis spilleren ikke valgte
    private int points;
    private CoachResponse feedback;
}
