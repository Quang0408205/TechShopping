package com.example.Tech.service.chat;

import com.example.Tech.dto.request.chat.ChatRequest;
import com.example.Tech.dto.response.chat.AdminChatSessionResponse;
import com.example.Tech.dto.response.chat.ChatAction;
import com.example.Tech.dto.response.chat.ChatMessageResponse;
import com.example.Tech.dto.response.chat.ChatReplyResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.chat.ChatMessage;
import com.example.Tech.entity.chat.ChatSession;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.chat.ChatMessageRepository;
import com.example.Tech.repository.chat.ChatSessionRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chatbot conversations (report section 3.6.5): the backend keeps the session and its messages, sends the
 * question with the recent history and the customer's token to the Chatbot Service, and stores the answer.
 * No transaction spans the LLM call (it can take many seconds): each save is its own short transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    static final int HISTORY_MESSAGES = 12;
    /** Context only needs the gist of a long earlier answer; also keeps the request under the AI service limits. */
    static final int HISTORY_MESSAGE_CHARS = 4000;
    static final int TOPIC_LENGTH = 255;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final AiChatClient aiChatClient;
    private final ChatRateLimiter rateLimiter;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /** A question that passed the checks and is stored, ready to be sent to the Chatbot Service. */
    public record Turn(ChatSession session, Long userId, String message, List<AiChatClient.Turn> history) {
    }

    /** Answer of an AI service SSE event (delta / status / done / error). */
    record AiEvent(String type, String text, String reply, List<Long> productIds, List<ChatAction> actions,
                   List<String> toolsUsed, String message) {
    }

    public ChatReplyResponse send(Long userId, String accessToken, String clientIp, ChatRequest request) {
        Turn turn = begin(userId, clientIp, request);
        AiChatClient.Answer answer = aiChatClient.ask(turn.message(), turn.history(), accessToken)
                .orElseThrow(ChatService::unavailable);
        return finish(turn, answer);
    }

    /**
     * Rate limit, session and the customer's message, before any byte of the stream is sent: these errors (429,
     * 404) still reach the browser as normal JSON errors.
     */
    public Turn begin(Long userId, String clientIp, ChatRequest request) {
        rateLimiter.acquire(userId, clientIp);
        String message = request.message().trim();
        ChatSession session = request.sessionToken() == null || request.sessionToken().isBlank()
                ? sessionRepository.save(new ChatSession(userId, newToken(), abbreviate(message), now()))
                : ownedSession(userId, request.sessionToken());

        List<AiChatClient.Turn> history = recentMessages(session.getId()).stream()
                .map(m -> new AiChatClient.Turn(m.fromUser() ? "user" : "assistant",
                        m.getContent().length() <= HISTORY_MESSAGE_CHARS ? m.getContent()
                                : m.getContent().substring(0, HISTORY_MESSAGE_CHARS) + "…"))
                .toList();
        messageRepository.save(new ChatMessage(session.getId(), ChatMessage.SENDER_USER, message, now()));
        return new Turn(session, userId, message, history);
    }

    /**
     * Streams the answer as Server-Sent Events: first {"type":"session"}, then the AI service's "delta" and
     * "status" events as they come, and last {"type":"done"} with the stored reply, product cards and actions
     * (or {"type":"error"} when the AI service is down, so the page can fall back to keyword answers).
     */
    public void stream(Turn turn, String accessToken, OutputStream out) throws IOException {
        write(out, Map.of("type", "session", "sessionToken", turn.session().getToken()));
        AiEvent[] done = new AiEvent[1];
        boolean reached = aiChatClient.stream(turn.message(), turn.history(), accessToken, json -> {
            AiEvent event = jsonMapper.readValue(json, AiEvent.class);
            if ("done".equals(event.type())) {
                done[0] = event;
            } else {
                writeRaw(out, json);
            }
        });
        if (!reached || done[0] == null) {
            write(out, Map.of("type", "error", "message", "Trợ lý AI đang tạm ngưng, vui lòng thử lại sau"));
            return;
        }
        AiEvent event = done[0];
        ChatReplyResponse reply = finish(turn,
                new AiChatClient.Answer(event.reply(), event.productIds(), event.actions(), event.toolsUsed()));
        Map<String, Object> last = new HashMap<>();
        last.put("type", "done");
        last.put("sessionToken", reply.sessionToken());
        last.put("reply", reply.reply());
        last.put("products", reply.products());
        last.put("actions", reply.actions());
        write(out, last);
    }

    private ChatReplyResponse finish(Turn turn, AiChatClient.Answer answer) {
        ChatSession session = turn.session();
        messageRepository.save(new ChatMessage(session.getId(), ChatMessage.SENDER_BOT, answer.reply(), now()));
        session.setLastMessageAt(now());
        sessionRepository.save(session);
        log.info("Chat session id={} user={} tools={}", session.getId(), turn.userId(), answer.toolsUsed());

        List<Long> productIds = answer.productIds() != null ? answer.productIds() : List.of();
        List<ChatAction> actions = answer.actions() != null ? answer.actions() : List.of();
        return new ChatReplyResponse(session.getToken(), answer.reply(), productService.getActiveByIds(productIds),
                actions);
    }

    private void write(OutputStream out, Object event) {
        writeRaw(out, jsonMapper.writeValueAsString(event));
    }

    private static void writeRaw(OutputStream out, String json) {
        try {
            out.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Trợ lý AI đang tạm ngưng, vui lòng thử lại sau");
    }

    /** Messages of my conversation, to show it again after a page reload. */
    public List<ChatMessageResponse> messages(Long userId, String token) {
        return messageRepository.findAllBySessionIdOrderByIdAsc(ownedSession(userId, token).getId()).stream()
                .map(ChatService::toResponse)
                .toList();
    }

    public PageResponse<AdminChatSessionResponse> adminSessions(Pageable pageable) {
        Page<ChatSession> page = sessionRepository.findAll(pageable);
        List<Long> ids = page.getContent().stream().map(ChatSession::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : messageRepository.countBySessionIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        Map<Long, User> users = userRepository.findAllById(page.getContent().stream()
                        .map(ChatSession::getUserId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return PageResponse.from(page.map(s -> {
            User user = s.getUserId() != null ? users.get(s.getUserId()) : null;
            return new AdminChatSessionResponse(s.getId(), s.getUserId(),
                    user != null ? user.getFullname() : "Khách chưa đăng nhập",
                    user != null ? user.getEmail() : null, s.getTopic(), counts.getOrDefault(s.getId(), 0L),
                    s.getCreatedAt(), s.getLastMessageAt());
        }));
    }

    public List<ChatMessageResponse> adminMessages(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException(ErrorCode.CHAT_SESSION_NOT_FOUND, sessionId);
        }
        return messageRepository.findAllBySessionIdOrderByIdAsc(sessionId).stream().map(ChatService::toResponse).toList();
    }

    /**
     * A customer's conversation is only theirs; a guest conversation is open to whoever holds its token and is
     * attached to the account when the guest logs in and keeps chatting.
     */
    private ChatSession ownedSession(Long userId, String token) {
        ChatSession session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CHAT_SESSION_NOT_FOUND, token));
        if (session.getUserId() == null && userId != null) {
            session.setUserId(userId);
            return sessionRepository.save(session);
        }
        if (session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new ResourceNotFoundException(ErrorCode.CHAT_SESSION_NOT_FOUND, token);
        }
        return session;
    }

    private List<ChatMessage> recentMessages(Long sessionId) {
        List<ChatMessage> newestFirst = new ArrayList<>(
                messageRepository.findBySessionIdOrderByIdDesc(sessionId, PageRequest.of(0, HISTORY_MESSAGES)));
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    private static ChatMessageResponse toResponse(ChatMessage m) {
        return new ChatMessageResponse(m.getSender(), m.getContent(), m.getSentAt());
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String abbreviate(String text) {
        return text.length() <= TOPIC_LENGTH ? text : text.substring(0, TOPIC_LENGTH - 1) + "…";
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
