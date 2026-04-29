package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// OP.GG MCP returnerer JSON-RPC 2.0 svar i dette format:
// { "jsonrpc": "2.0", "result": { "content": [{ "type": "text", "text": "..." }] }, "id": 1 }
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpResponse {

    private McpResult result;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpResult {
        private List<McpContent> content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McpContent {
        private String type;
        private String text;
    }

    /**
     * Samler al tekst-indhold fra MCP-svaret til én streng.
     */
    public String extractText() {
        if (result == null || result.getContent() == null) return "";

        StringBuilder sb = new StringBuilder();
        for (McpContent item : result.getContent()) {
            if ("text".equals(item.getType())) {
                sb.append(item.getText());
            }
        }
        return sb.toString();
    }
}
