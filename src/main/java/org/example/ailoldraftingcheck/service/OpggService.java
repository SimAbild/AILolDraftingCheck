package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

// Kommunikerer med OP.GG's MCP-endpoint for at hente champion-statistikker:
// winrates mod specifikke champions, synergier ("works with") og counters ("strong against").
// MCP bruger JSON-RPC 2.0 over HTTP POST — ingen API-nøgle kræves.

@Service
public class OpggService {

    private static final Logger logger = LoggerFactory.getLogger(OpggService.class);
    private static final String OPGG_MCP_URL = "https://mcp-api.op.gg/mcp";

    private final WebClient webClient;

    public OpggService() {
        this.webClient = WebClient.builder().build();
    }

    // Henter champion-analyse inklusiv winrates mod specifikke modstandere.
    // Returnerer rå tekst fra OP.GG som kan sendes videre til OpenAI for scoring.
    public String getChampionAnalysis(String championName, String role) {
        return callMcpTool("lol-champion-analysis", Map.of(
                "champion_name", championName,
                "role", role.toLowerCase()
        ));
    }

    // Henter meta-data inklusiv "works with" (synergier) og "strong against" (counters).
    public String getChampionMetaData(String championName, String role) {
        return callMcpTool("lol-champion-meta-data", Map.of(
                "champion_name", championName,
                "role", role.toLowerCase()
        ));
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
            McpResponse response = webClient.post()
                    .uri(OPGG_MCP_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(McpResponse.class)
                    .block();

            return response != null ? response.extractText() : "";
        } catch (Exception e) {
            logger.error("OP.GG MCP-kald fejlede for tool '{}': {}", toolName, e.getMessage());
            return "";
        }
    }
}
