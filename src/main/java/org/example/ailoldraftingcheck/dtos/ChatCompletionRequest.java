package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Denne klasse forbliver en POJO (ikke en record) fordi messages-listen
// bygges op trinvist med .add() i OpenAiService — records er uforanderlige.
@Getter
@Setter
public class ChatCompletionRequest {

    private String model;
    private List<Message> messages = new ArrayList<>();
    private double temperature;

    // @JsonProperty bruges her fordi OpenAI's API forventer snake_case feltnavne i JSON,
    // men Java-konventionen er camelCase. Annotationen fortæller Jackson:
    // "serialisér dette Java-felt som 'max_tokens' i JSON-outputtet."
    // Uden @JsonProperty ville Jackson sende "maxTokens", som OpenAI ikke forstår.
    @JsonProperty("max_tokens")
    private int maxTokens;

    @JsonProperty("top_p")
    private double topP;

    @JsonProperty("frequency_penalty")
    private double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private double presencePenalty;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Getter
    public static class ResponseFormat {
        private final String type;

        public ResponseFormat(String type) {
            this.type = type;
        }
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
