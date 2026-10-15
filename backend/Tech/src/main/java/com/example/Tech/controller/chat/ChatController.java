package com.example.Tech.controller.chat;

import com.example.Tech.dto.request.chat.ChatRequest;
import com.example.Tech.dto.response.chat.ChatMessageResponse;
import com.example.Tech.dto.response.chat.ChatReplyResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

/**
 * AI shopping assistant. Public: guests can ask general questions; with an access token the assistant can also
 * read the customer's own orders, cart and service requests (through the normal APIs, with that token).
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "AI assistant: RAG over the shop policies / catalogue + function calling")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Send a message and get the assistant's answer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "CHAT_SESSION_NOT_FOUND"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS"),
            @ApiResponse(responseCode = "503", description = "AI_SERVICE_UNAVAILABLE")
    })
    public ResponseEntity<ApiResult<ChatReplyResponse>> send(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody ChatRequest request,
                                                             HttpServletRequest httpRequest) {
        Long userId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        String token = jwt != null ? jwt.getTokenValue() : null;
        return ResponseEntity.ok(ApiResult.ok(chatService.send(userId, token, httpRequest.getRemoteAddr(), request)));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message; the answer is streamed as Server-Sent Events (session, delta, status, "
            + "done | error)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event stream"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "CHAT_SESSION_NOT_FOUND"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS")
    })
    public ResponseEntity<StreamingResponseBody> stream(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody ChatRequest request,
                                                        HttpServletRequest httpRequest) {
        Long userId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        String token = jwt != null ? jwt.getTokenValue() : null;
        ChatService.Turn turn = chatService.begin(userId, httpRequest.getRemoteAddr(), request);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(out -> chatService.stream(turn, token, out));
    }

    @GetMapping("/{sessionToken}/messages")
    @Operation(summary = "Messages of my conversation (to show it again after a reload)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages, oldest first"),
            @ApiResponse(responseCode = "404", description = "CHAT_SESSION_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<List<ChatMessageResponse>>> messages(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable String sessionToken) {
        Long userId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        return ResponseEntity.ok(ApiResult.ok(chatService.messages(userId, sessionToken)));
    }
}
