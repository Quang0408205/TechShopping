package com.example.Tech.controller.chat;

import com.example.Tech.dto.response.chat.AdminChatSessionResponse;
import com.example.Tech.dto.response.chat.ChatMessageResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Chatbot conversations, read-only, to monitor answer quality (ADMIN, see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/chat-sessions")
@RequiredArgsConstructor
@Tag(name = "Admin - Chatbot history", description = "Read-only chatbot conversations")
public class AdminChatController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "Conversations, most recent activity first")
    @ApiResponse(responseCode = "200", description = "Page of conversations")
    public ResponseEntity<ApiResult<PageResponse<AdminChatSessionResponse>>> list(
            @ParameterObject @PageableDefault(size = 30, sort = "lastMessageAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(chatService.adminSessions(pageable)));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Messages of one conversation, oldest first")
    @ApiResponse(responseCode = "404", description = "CHAT_SESSION_NOT_FOUND")
    public ResponseEntity<ApiResult<List<ChatMessageResponse>>> messages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(chatService.adminMessages(id)));
    }
}
