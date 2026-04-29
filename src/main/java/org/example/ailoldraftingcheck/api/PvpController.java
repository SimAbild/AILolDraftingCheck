package org.example.ailoldraftingcheck.api;

import org.example.ailoldraftingcheck.dtos.pvp.PvpChampionSelectRequest;
import org.example.ailoldraftingcheck.dtos.pvp.PvpFindMatchRequest;
import org.example.ailoldraftingcheck.service.PvpGameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class PvpController {

    private final PvpGameService pvpGameService;

    public PvpController(PvpGameService pvpGameService) {
        this.pvpGameService = pvpGameService;
    }

    @MessageMapping("/pvp/find-match")
    public void findMatch(PvpFindMatchRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        pvpGameService.findMatch(sessionId, request.getUsername());
    }

    @MessageMapping("/pvp/champion-selected")
    public void championSelected(PvpChampionSelectRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        pvpGameService.selectChampion(sessionId, request.getRoomId(), request.getChampion());
    }
}
