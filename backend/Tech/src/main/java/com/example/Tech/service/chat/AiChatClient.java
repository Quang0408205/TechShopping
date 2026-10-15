package com.example.Tech.service.chat;

import com.example.Tech.dto.response.chat.ChatAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Calls POST /chat of the Chatbot Service (report table 3.25). Empty when it does not answer. */
@Slf4j
@Component
public class AiChatClient {

    public record Turn(String role, String content) {
    }

    public record Answer(String reply, List<Long> productIds, List<ChatAction> actions, List<String> toolsUsed) {
    }

    private final RestClient restClient;

    public AiChatClient(@Qualifier("aiChatRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param accessToken the customer's own JWT, so the chatbot's function calls reach the business APIs with the
     *                    customer's permissions; null for a guest
     */
    public Optional<Answer> ask(String message, List<Turn> history, String accessToken) {
        Map<String, Object> body = body(message, history, accessToken);
        try {
            return Optional.ofNullable(restClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Answer.class));
        } catch (RestClientException exception) {
            log.warn("AI service /chat failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * POST /chat/stream: passes the JSON of every Server-Sent Event ("data: {...}") to onEvent as it arrives.
     * Returns false when the AI service could not be reached or refused the request (nothing was passed on).
     */
    public boolean stream(String message, List<Turn> history, String accessToken, Consumer<String> onEvent) {
        try {
            return Boolean.TRUE.equals(restClient.post()
                    .uri("/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body(message, history, accessToken))
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            log.warn("AI service /chat/stream answered {}", response.getStatusCode());
                            return false;
                        }
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    onEvent.accept(line.substring("data: ".length()));
                                }
                            }
                        }
                        return true;
                    }));
        } catch (RestClientException exception) {
            log.warn("AI service /chat/stream failed: {}", exception.getMessage());
            return false;
        }
    }

    private static Map<String, Object> body(String message, List<Turn> history, String accessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("history", history);
        body.put("accessToken", accessToken);
        return body;
    }
}
