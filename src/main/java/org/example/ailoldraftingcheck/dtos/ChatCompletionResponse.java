package org.example.ailoldraftingcheck.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/*
 Describes the JSON we get back from OpenAI. We only care about
 choices[0].message.content, but we include the other fields so Jackson
 can deserialize the full payload without complaining.
 Copied from the chatgpt-jokes example.
*/
@Getter
@Setter
public class ChatCompletionResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    // One answer from the model. choices[0] is the one we use.
    @Getter
    @Setter
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;
    }

    // The actual reply text sits inside Message.content.
    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
    }

    // Lets us log how many tokens each call cost.
    @Getter
    @Setter
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }
}
