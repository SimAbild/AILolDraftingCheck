package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.ChatCompletionRequest;
import org.example.ailoldraftingcheck.dtos.ChatCompletionResponse;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/**
 * Thin wrapper around the OpenAI Chat Completions endpoint.
 *
 * Based on the chatgpt-jokes example. Uses WebClient to call the external API
 * and .block() to bridge reactive code to our normal imperative controllers.
 */
@Service
public class OpenAiService {

    public static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    // Config values are injected from application.properties.
    @Value("${app.api-key}")
    private String API_KEY;

    @Value("${app.url}")
    private String URL;

    @Value("${app.model}")
    private String MODEL;

    @Value("${app.temperature}")
    private double TEMPERATURE;

    @Value("${app.max_tokens}")
    private int MAX_TOKENS;

    @Value("${app.frequency_penalty}")
    private double FREQUENCY_PENALTY;

    @Value("${app.presence_penalty}")
    private double PRESENCE_PENALTY;

    @Value("${app.top_p}")
    private double TOP_P;

    // One shared WebClient instance, built with WebClient.builder().
    private final WebClient client;

    // Jackson 3: use JsonMapper.builder().build() instead of new ObjectMapper().
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public OpenAiService() {
        this.client = WebClient.builder().build();
    }

    /**
     * Sends a system + user message to ChatGPT and returns the raw text reply.
     *
     * Steps:
     *   1. Build a ChatCompletionRequest DTO with the model + tuning knobs.
     *   2. Convert it to JSON with Jackson.
     *   3. POST it to the OpenAI URL, with the API key in the Authorization header.
     *   4. Parse the response into ChatCompletionResponse and return the text.
     */
    public String chat(String systemMessage, String userMessage) {

        // 1. Build the request body.
        ChatCompletionRequest requestDto = new ChatCompletionRequest();
        requestDto.setModel(MODEL);
        requestDto.setTemperature(TEMPERATURE);
        requestDto.setMax_tokens(MAX_TOKENS);
        requestDto.setTop_p(TOP_P);
        requestDto.setFrequency_penalty(FREQUENCY_PENALTY);
        requestDto.setPresence_penalty(PRESENCE_PENALTY);
        requestDto.getMessages().add(new ChatCompletionRequest.Message("system", systemMessage));
        requestDto.getMessages().add(new ChatCompletionRequest.Message("user", userMessage));

        try {
            // 2. Convert DTO -> JSON string.
            String json = mapper.writeValueAsString(requestDto);

            // 3. POST to OpenAI. .block() waits for the response before returning.
            ChatCompletionResponse response = client.post()
                    .uri(new URI(URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(json))
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            // 4. Pull the actual text reply out of choices[0].message.content.
            int tokensUsed = response.getUsage().getTotal_tokens();
            logger.info("OpenAI tokens used: " + tokensUsed);
            return response.getChoices().get(0).getMessage().getContent();

        } catch (WebClientResponseException e) {
            // The API responded with an error status (bad API key, quota, etc.).
            logger.error("OpenAI error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to reach OpenAI. Check the backend log and your API_KEY.");
        } catch (Exception e) {
            // Anything else (network, JSON parse, etc.)
            logger.error("OpenAI unexpected error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal error while talking to OpenAI.");
        }
    }
}
