package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

// Kommunikerer med OP.GG's MCP-endpoint for at hente champion-statistikker:
// winrates mod specifikke champions, synergier og counters.
// MCP bruger JSON-RPC 2.0 over HTTP POST — ingen API-nøgle kræves.

@Service
public class OpggService {

    private static final Logger logger = LoggerFactory.getLogger(OpggService.class);
    private static final String OPGG_MCP_URL = "https://mcp-api.op.gg/mcp";

    private final WebClient webClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OpggService() {
        this.webClient = WebClient.builder().build();
    }

    // Henter champion-analyse inklusiv winrates mod specifikke modstandere.
    // Returnerer rå tekst fra OP.GG som kan sendes videre til OpenAI for scoring.
    public String getChampionAnalysis(String championName, String role) {
        return callMcpTool("lol_get_champion_analysis", Map.of(
                "champion", championName,
                "position", mapRole(role),
                "game_mode", "ranked",
                "desired_output_fields", "data.{counterChampions,banPickData}"
        ));
    }

    // Henter specifik matchup-data mellem to champions i en given rolle.
    // Ingen desired_output_fields — vi henter alt data og lader OpenAI finde winrate.
    public String getMatchupData(String championName, String enemyName, String role) {
        return callMcpTool("lol_get_lane_matchup_guide", Map.of(
                "champion", championName,
                "opponent", enemyName,
                "position", mapRole(role),
                "game_mode", "ranked"
        ));
    }

    // Henter meta-data inklusiv synergier og counters for en champion i en given rolle.
    public String getChampionMetaData(String championName, String role) {
        return callMcpTool("lol_list_lane_meta_champions", Map.of(
                "champion", championName,
                "position", mapRole(role),
                "game_mode", "ranked",
                "desired_output_fields", "data.champions[].{name,winRate,pickRate,tier}"
        ));
    }

    // OP.GG bruger "jungle" i stedet for "jgl".
    private String mapRole(String role) {
        if ("JGL".equalsIgnoreCase(role)) return "jungle";
        return role.toLowerCase();
    }

    // Kalder et OP.GG MCP-tool via JSON-RPC 2.0 og returnerer tekst-indholdet.
    // Ved fejl returneres en tom streng — kaldende kode skal håndtere dette.
    private String callMcpTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", Map.of("name", toolName, "arguments", arguments),
                "id", 1
        );

        try {
            String rawBody = webClient.post()
                    .uri(OPGG_MCP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("OP.GG '{}' svar (laengde: {})", toolName, rawBody != null ? rawBody.length() : 0);
            if ("lol_get_lane_matchup_guide".equals(toolName) && rawBody != null) {
                logger.info("OP.GG matchup rå JSON (forkortet): {}",
                        rawBody.substring(0, Math.min(rawBody.length(), 800)));
            }

            if (rawBody == null || rawBody.isEmpty()) return "";

            McpResponse response = jsonMapper.readValue(rawBody, McpResponse.class);
            return response.extractText();
        } catch (Exception e) {
            logger.error("OP.GG MCP-kald fejlede for tool '{}': {}", toolName, e.getMessage());
            return "";
        }
    }
}
