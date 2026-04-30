package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.DraftResponse;
import org.example.ailoldraftingcheck.dtos.pvp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Håndterer PvP-matchmaking, countdown og game-flow.
// Matchmaking: spillere tilføjes til en kø, og når to er klar, oprettes et spil.
// Countdown: serveren styrer en 60-sekunders timer og sender ticks til begge spillere.
// Scoring er uddelegeret til PvpScoringService (Single Responsibility).

@Service
public class PvpGameService {

    private static final Logger logger = LoggerFactory.getLogger(PvpGameService.class);
    private static final List<String> VALID_ROLES = List.of("TOP", "JGL", "MID", "ADC", "SUPP");
    private static final int COUNTDOWN_SECONDS = 60;
    private static final int ROOM_ID_LENGTH = 4;

    private final ConcurrentLinkedQueue<PvpPlayer> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, PvpGame> activeGames = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final SimpMessagingTemplate messagingTemplate;
    private final DraftService draftService;
    private final PvpScoringService scoringService;

    public PvpGameService(SimpMessagingTemplate messagingTemplate,
                          DraftService draftService,
                          PvpScoringService scoringService) {
        this.messagingTemplate = messagingTemplate;
        this.draftService = draftService;
        this.scoringService = scoringService;
    }

    // ── Matchmaking ─────────────────────────────────────────────────────

    public void findMatch(String sessionId, String username) {
        waitingPlayers.add(new PvpPlayer(sessionId, username, null));
        logger.info("'{}' joined queue. Queue size: {}", username, waitingPlayers.size());

        sendToPlayer(sessionId, "/queue/pvp/status",
                Map.of("status", "waiting", "message", "Soeger efter modstander..."));

        tryCreateMatch();
    }

    private synchronized void tryCreateMatch() {
        if (waitingPlayers.size() < 2) return;

        PvpPlayer player1 = waitingPlayers.poll();
        PvpPlayer player2 = waitingPlayers.poll();
        if (player1 == null || player2 == null) return;

        String roomId = generateRoomId();
        String role = pickRandomRole();
        PvpGame game = new PvpGame(roomId, player1, player2, role);

        logger.info("Match: {} vs {} | Room: {} | Role: {}",
                player1.getUsername(), player2.getUsername(), roomId, role);

        // Draft genereres asynkront for ikke at blokere WebSocket-tråden.
        scheduler.execute(() -> initializeGame(game));
    }

    private void initializeGame(PvpGame game) {
        try {
            DraftResponse draft = draftService.generateDraft(game.getAssignedRole());
            game.setEnemyTeam(draft.getEnemy());
            game.setAllyTeam(draft.getAlly());
            activeGames.put(game.getRoomId(), game);

            PvpMatchFoundMessage message = new PvpMatchFoundMessage(
                    game.getRoomId(), game.getAssignedRole(),
                    draft.getEnemy(), draft.getAlly(),
                    game.getPlayer1().getUsername(), game.getPlayer2().getUsername()
            );

            sendToBothPlayers(game, "/queue/pvp/match-found", message);
            startCountdown(game.getRoomId());
        } catch (Exception e) {
            logger.error("Kunne ikke oprette match for room {}: {}", game.getRoomId(), e.getMessage());
            sendToBothPlayers(game, "/queue/pvp/error",
                    Map.of("message", "Kunne ikke oprette match. Proev igen."));
        }
    }

    // ── Champion-valg ───────────────────────────────────────────────────

    public void selectChampion(String sessionId, String roomId, String champion) {
        PvpGame game = activeGames.get(roomId);
        if (game == null) return;

        PvpPlayer player = game.getPlayerBySessionId(sessionId);
        if (player == null) return;

        player.setChampion(champion);
        logger.info("'{}' valgte '{}' i room {}", player.getUsername(), champion, roomId);

        sendToPlayer(sessionId, "/queue/pvp/champion-confirmed", Map.of("champion", champion));
    }

    // ── Countdown ───────────────────────────────────────────────────────

    private void startCountdown(String roomId) {
        AtomicInteger secondsLeft = new AtomicInteger(COUNTDOWN_SECONDS);

        ScheduledFuture<?> ticker = scheduler.scheduleAtFixedRate(() -> {
            PvpGame game = activeGames.get(roomId);
            if (game == null) return;

            int remaining = secondsLeft.decrementAndGet();
            sendToBothPlayers(game, "/queue/pvp/countdown", Map.of("seconds", remaining));

        }, 1, 1, TimeUnit.SECONDS);

        // Når countdown udløber: stop ticker, reveal valg, beregn resultater.
        scheduler.schedule(() -> {
            ticker.cancel(false);
            onCountdownFinished(roomId);
        }, COUNTDOWN_SECONDS, TimeUnit.SECONDS);
    }

    private void onCountdownFinished(String roomId) {
        PvpGame game = activeGames.get(roomId);
        if (game == null) return;

        game.setCountdownFinished(true);

        // Reveal begge spilleres valg (points=0, feedback=null — endnu ikke beregnet)
        PvpRoundMessage reveal = new PvpRoundMessage(
                new PvpPlayerResult(game.getPlayer1().getUsername(), game.getPlayer1().getChampion(), 0, null),
                new PvpPlayerResult(game.getPlayer2().getUsername(), game.getPlayer2().getChampion(), 0, null)
        );
        sendToBothPlayers(game, "/queue/pvp/reveal", reveal);

        // Beregn resultater asynkront (kræver OpenAI + OP.GG kald)
        scheduler.execute(() -> calculateAndSendResults(game));
    }

    private void calculateAndSendResults(PvpGame game) {
        try {
            PvpPlayerResult result1 = scoringService.calculateResult(game, game.getPlayer1());
            PvpPlayerResult result2 = scoringService.calculateResult(game, game.getPlayer2());
            PvpRoundMessage results = new PvpRoundMessage(result1, result2);

            sendToBothPlayers(game, "/queue/pvp/results", results);
            activeGames.remove(game.getRoomId());
        } catch (Exception e) {
            logger.error("Kunne ikke beregne resultater for room {}: {}", game.getRoomId(), e.getMessage());
        }
    }

    // ── Disconnect ──────────────────────────────────────────────────────

    public void handleDisconnect(String sessionId) {
        waitingPlayers.removeIf(p -> p.getSessionId().equals(sessionId));

        activeGames.forEach((roomId, game) -> {
            PvpPlayer opponent = getOpponent(game, sessionId);
            if (opponent != null) {
                sendToPlayer(opponent.getSessionId(), "/queue/pvp/opponent-disconnected",
                        Map.of("message", "Din modstander har forladt spillet."));
                activeGames.remove(roomId);
            }
        });
    }

    // ── Hjælpemetoder ───────────────────────────────────────────────────

    private void sendToPlayer(String sessionId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
    }

    private void sendToBothPlayers(PvpGame game, String destination, Object payload) {
        sendToPlayer(game.getPlayer1().getSessionId(), destination, payload);
        sendToPlayer(game.getPlayer2().getSessionId(), destination, payload);
    }

    private PvpPlayer getOpponent(PvpGame game, String sessionId) {
        if (game.getPlayer1().getSessionId().equals(sessionId)) return game.getPlayer2();
        if (game.getPlayer2().getSessionId().equals(sessionId)) return game.getPlayer1();
        return null;
    }

    private String pickRandomRole() {
        return VALID_ROLES.get(ThreadLocalRandom.current().nextInt(VALID_ROLES.size()));
    }

    private String generateRoomId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("DRAFT-");
        for (int i = 0; i < ROOM_ID_LENGTH; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
