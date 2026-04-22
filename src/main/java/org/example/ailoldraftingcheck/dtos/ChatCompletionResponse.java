package org.example.ailoldraftingcheck.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// OpenAI's API svarer med snake_case feltnavne.
// @JsonProperty bruges på hvert felt der afviger fra Java's camelCase-konvention,
// så Jackson kan mappe korrekt under deserialisering (JSON → Java).
@Getter
@Setter
public class ChatCompletionResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Getter
    @Setter
    public static class Choice {
        private int index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
    }

    @Getter
    @Setter
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
