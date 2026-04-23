package org.example.ailoldraftingcheck.service;

import org.example.ailoldraftingcheck.dtos.ChatCompletionRequest;
import org.example.ailoldraftingcheck.dtos.ChatCompletionResponse;
import tools.jackson.databind.JsonNode;
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

@Service
public class OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);
    private static final int FIRST_CHOICE_INDEX = 0;

    @Value("${app.api-key}")
    private String apiKey;

    @Value("${app.url}")
    private String apiUrl;

    @Value("${app.model}")
    private String model;

    @Value("${app.temperature}")
    private double temperature;

    @Value("${app.max_tokens}")
    private int maxTokens;

    @Value("${app.frequency_penalty}")
    private double frequencyPenalty;

    @Value("${app.presence_penalty}")
    private double presencePenalty;

    @Value("${app.top_p}")
    private double topP;

    private final WebClient webClient;

    private final ObjectMapper jsonMapper;

    public OpenAiService() {
        this.webClient = WebClient.builder().build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    public String chat(String systemMessage, String userMessage) {
        ChatCompletionRequest chatRequest = buildChatRequest(systemMessage, userMessage);

        try {
            String requestBodyJson = jsonMapper.writeValueAsString(chatRequest);
            ChatCompletionResponse chatResponse = sendChatRequest(requestBodyJson);
            return extractReplyText(chatResponse);
        } catch (WebClientResponseException e) {
            logger.error("OpenAI error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to reach OpenAI. Check the backend log and your API_KEY.");
        } catch (Exception e) {
            logger.error("OpenAI unexpected error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal error while talking to OpenAI.");
        }
    }

    private ChatCompletionRequest buildChatRequest(String systemMessage, String userMessage) {
        ChatCompletionRequest chatRequest = new ChatCompletionRequest();
        chatRequest.setModel(model);
        chatRequest.setTemperature(temperature);
        chatRequest.setMax_tokens(maxTokens);
        chatRequest.setTop_p(topP);
        chatRequest.setFrequency_penalty(frequencyPenalty);
        chatRequest.setPresence_penalty(presencePenalty);
        chatRequest.getMessages().add(new ChatCompletionRequest.Message("system", systemMessage));
        chatRequest.getMessages().add(new ChatCompletionRequest.Message("user", userMessage));
        return chatRequest;
    }

    private ChatCompletionResponse sendChatRequest(String requestBodyJson) throws Exception {
        return webClient.post()
                .uri(new URI(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBodyJson))
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();
    }

    private String extractReplyText(ChatCompletionResponse chatResponse) {
        int tokensUsed = chatResponse.getUsage().getTotal_tokens();
        logger.info("OpenAI tokens used: {}", tokensUsed);
        return chatResponse.getChoices().get(FIRST_CHOICE_INDEX).getMessage().getContent();
    }

    public JsonNode parseAiReply(String content) throws Exception {
        String cleanContent = content.trim();
        if (cleanContent.startsWith("```")) {
            cleanContent = cleanContent.replaceAll("(?s)```(json)?", "").trim();
        }
        return jsonMapper.readTree(cleanContent);
    }
}
