package org.example.ailoldraftingcheck.config;

import org.example.ailoldraftingcheck.service.PvpGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PvpGameService pvpGameService;

    public WebSocketEventListener(PvpGameService pvpGameService) {
        this.pvpGameService = pvpGameService;
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        logger.info("WebSocket disconnected: {}", sessionId);
        pvpGameService.handleDisconnect(sessionId);
    }
}
