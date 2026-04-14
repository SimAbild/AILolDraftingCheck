package org.example.ailoldraftingcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ailoldraftingcheck.dtos.ChatCompletionRequest;
import org.example.ailoldraftingcheck.dtos.ChatCompletionResponse;
import org.example.ailoldraftingcheck.entity.ApiUsage;
import org.example.ailoldraftingcheck.entity.ApiUsageRepository;
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
 * Mirrors the example chatgpt-jokes service: WebClient + .block() bridge,
 * config bound from application.properties, error mapping to a Spring
 * ResponseStatusException so controllers can stay simple.
 *
 * Adds: persistent token-usage logging via {@link ApiUsageRepository}, and a
 * raw-content method (not wrapped in MyResponse) because the draft/coach
 * endpoints want to parse the LLM's reply as JSON themselves.
 */
@Service
public class OpenAiService {

    public static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

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

    private final WebClient client;
    private final ApiUsageRepository usageRepository;

    public OpenAiService(ApiUsageRepository usageRepository) {
        this.client = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.usageRepository = usageRepository;
    }

    /** Test-only constructor: lets unit tests inject a mock WebClient. */
    public OpenAiService(WebClient client, ApiUsageRepository usageRepository) {
        this.client = client;
        this.usageRepository = usageRepository;
    }

    /**
     * Send a system + user message pair to ChatGPT and return the raw assistant
     * content string. Token usage is logged to the DB tagged with {@code endpointTag}.
     */
    public String chat(String systemMessage, String userMessage, String endpointTag) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(MODEL);
        request.setTemperature(TEMPERATURE);
        request.setMax_tokens(MAX_TOKENS);
        request.setTop_p(TOP_P);
        request.setFrequency_penalty(FREQUENCY_PENALTY);
        request.setPresence_penalty(PRESENCE_PENALTY);
        request.getMessages().add(new ChatCompletionRequest.Message("system", systemMessage));
        request.getMessages().add(new ChatCompletionRequest.Message("user", userMessage));

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(request);
            ChatCompletionResponse response = client.post()
                    .uri(new URI(URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(json))
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            String content = response.getChoices().get(0).getMessage().getContent();
            ChatCompletionResponse.Usage u = response.getUsage();
            usageRepository.save(new ApiUsage(
                    endpointTag, u.getPrompt_tokens(), u.getCompletion_tokens(), u.getTotal_tokens()));
            logger.info("OpenAI [{}] tokens used: {}", endpointTag, u.getTotal_tokens());
            return content;
        } catch (WebClientResponseException e) {
            logger.error("OpenAI error {} - body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to reach OpenAI. Check the backend logs and your API_KEY.");
        } catch (Exception e) {
            logger.error("OpenAI unexpected error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal error while talking to OpenAI: " + e.getMessage());
        }
    }
}
