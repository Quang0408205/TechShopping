package com.example.Tech.service.chat;

import com.example.Tech.dto.request.chat.ChatRequest;
import com.example.Tech.dto.response.chat.ChatReplyResponse;
import com.example.Tech.entity.chat.ChatMessage;
import com.example.Tech.entity.chat.ChatSession;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.chat.ChatMessageRepository;
import com.example.Tech.repository.chat.ChatSessionRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private ChatRateLimiter rateLimiter;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(sessionRepository, messageRepository, userRepository, productService, aiChatClient,
                rateLimiter, JsonMapper.builder().build(), Clock.systemDefaultZone());
    }

    @Test
    void newConversation_forwardsTheTokenAndStoresBothMessages() {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(5L);
            return session;
        });
        when(aiChatClient.ask(eq("Phí ship?"), eq(List.of()), eq("jwt-of-user")))
                .thenReturn(Optional.of(new AiChatClient.Answer("Phí 30.000đ.", List.of(), List.of(), List.of("search_policies"))));

        ChatReplyResponse reply = service.send(7L, "jwt-of-user", "1.2.3.4", new ChatRequest(null, "  Phí ship?  "));

        assertThat(reply.reply()).isEqualTo("Phí 30.000đ.");
        assertThat(reply.sessionToken()).hasSizeGreaterThanOrEqualTo(32);
        ArgumentCaptor<ChatMessage> messages = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(messages.capture());
        assertThat(messages.getAllValues()).extracting(ChatMessage::getSender)
                .containsExactly(ChatMessage.SENDER_USER, ChatMessage.SENDER_BOT);
        verify(rateLimiter).acquire(7L, "1.2.3.4");
    }

    @Test
    @SuppressWarnings("unchecked")
    void longEarlierAnswers_areShortenedInTheHistory() {
        ChatSession session = new ChatSession(7L, "my-token", "x", LocalDateTime.now());
        session.setId(5L);
        when(sessionRepository.findByToken("my-token")).thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByIdDesc(eq(5L), any()))
                .thenReturn(List.of(new ChatMessage(5L, ChatMessage.SENDER_BOT, "a".repeat(10_000), LocalDateTime.now())));
        when(aiChatClient.ask(any(), any(), any()))
                .thenReturn(Optional.of(new AiChatClient.Answer("ok", List.of(), List.of(), List.of())));

        service.send(7L, "jwt", "ip", new ChatRequest("my-token", "tiếp"));

        ArgumentCaptor<List<AiChatClient.Turn>> history = ArgumentCaptor.forClass(List.class);
        verify(aiChatClient).ask(eq("tiếp"), history.capture(), eq("jwt"));
        assertThat(history.getValue().get(0).content()).hasSize(ChatService.HISTORY_MESSAGE_CHARS + 1);
        assertThat(history.getValue().get(0).role()).isEqualTo("assistant");
    }

    @Test
    void stream_forwardsDeltas_storesTheReply_andEndsWithProductsAndActions() throws Exception {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiChatClient.stream(eq("Thêm iPhone vào giỏ"), any(), eq("jwt"), any())).thenAnswer(invocation -> {
            java.util.function.Consumer<String> onEvent = invocation.getArgument(3);
            onEvent.accept("{\"type\":\"delta\",\"text\":\"Bạn bấm \"}");
            onEvent.accept("{\"type\":\"status\",\"text\":\"Đang chuẩn bị thêm vào giỏ...\"}");
            onEvent.accept("{\"type\":\"done\",\"reply\":\"Bạn bấm Xác nhận nhé.\",\"productIds\":[],"
                    + "\"actions\":[{\"type\":\"ADD_TO_CART\",\"label\":\"Thêm\",\"variantId\":37,\"quantity\":1}],"
                    + "\"toolsUsed\":[\"propose_add_to_cart\"]}");
            return true;
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ChatService.Turn turn = service.begin(7L, "ip", new ChatRequest(null, "Thêm iPhone vào giỏ"));
        service.stream(turn, "jwt", out);

        String sse = out.toString(StandardCharsets.UTF_8);
        assertThat(sse).startsWith("data: {").contains("\"type\":\"session\"")
                .contains("\"text\":\"Bạn bấm \"").contains("Đang chuẩn bị thêm vào giỏ")
                .contains("\"type\":\"done\"").contains("\"variantId\":37");
        assertThat(sse.indexOf("\"type\":\"done\"")).isGreaterThan(sse.indexOf("\"type\":\"delta\""));
        ArgumentCaptor<ChatMessage> messages = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(messages.capture());
        assertThat(messages.getAllValues().get(1).getContent()).isEqualTo("Bạn bấm Xác nhận nhé.");
    }

    @Test
    void stream_aiDown_endsWithAnErrorEvent() throws Exception {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiChatClient.stream(any(), any(), any(), any())).thenReturn(false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.stream(service.begin(null, "ip", new ChatRequest(null, "Xin chào")), null, out);

        assertThat(out.toString(StandardCharsets.UTF_8)).contains("\"type\":\"error\"");
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void someoneElsesConversation_isNotFound() {
        ChatSession session = new ChatSession(8L, "token-of-8", "x", LocalDateTime.now());
        when(sessionRepository.findByToken("token-of-8")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.send(7L, "jwt", "ip", new ChatRequest("token-of-8", "Đơn của tôi?")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.messages(null, "token-of-8"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(aiChatClient, never()).ask(any(), any(), any());
    }

    @Test
    void guestConversation_isAttachedToTheAccountAfterLogin() {
        ChatSession session = new ChatSession(null, "guest-token", "x", LocalDateTime.now());
        session.setId(9L);
        when(sessionRepository.findByToken("guest-token")).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);

        service.messages(7L, "guest-token");

        assertThat(session.getUserId()).isEqualTo(7L);
    }

    @Test
    void aiDown_isServiceUnavailable_butTheQuestionIsKept() {
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiChatClient.ask(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(null, null, "ip", new ChatRequest(null, "Xin chào")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
        verify(messageRepository, times(1)).save(any(ChatMessage.class));
        verify(productService, never()).getActiveByIds(any());
        verify(messageRepository, never()).findAllBySessionIdOrderByIdAsc(anyLong());
    }
}
