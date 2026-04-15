package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 This class describes the JSON body we send to the OpenAI Chat Completions API.
 Field names are intentionally snake_case because that is what the API expects.
 Copied from the chatgpt-jokes example.
*/
@Getter
@Setter
public class ChatCompletionRequest {

    // Which model to use, e.g. "gpt-4o-mini".
    private String model;

    // The conversation we send to the model: a system message + a user message.
    private List<Message> messages = new ArrayList<>();

    // Tuning parameters (see application.properties for the values).
    private double temperature;
    private int max_tokens;
    private double top_p;
    private double frequency_penalty;
    private double presence_penalty;

    // One message in the conversation: role ("system" / "user") + content.
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
