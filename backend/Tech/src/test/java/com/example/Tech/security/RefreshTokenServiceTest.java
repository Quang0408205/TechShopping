package com.example.Tech.security;

import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration SEVEN_DAYS = Duration.ofDays(7);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("unit-test-secret-0123456789abcdef-xyz", "techshopping",
                Duration.ofMinutes(30), SEVEN_DAYS);
        service = new RefreshTokenService(redisTemplate, properties);
    }

    private static String hashOf(String token) {
        return RefreshTokenService.key(token).substring(RefreshTokenService.KEY_PREFIX.length());
    }

    @Test
    void issue_storesHashedKeyWithTtl_andIndexesItForTheUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        String token = service.issue(7L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("7"), eq(SEVEN_DAYS));
        String key = keyCaptor.getValue();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(key).isEqualTo(RefreshTokenService.key(token)).startsWith("auth:refresh:").doesNotContain(token);
        assertThat(key).hasSize("auth:refresh:".length() + 64);

        verify(setOperations).add("auth:user-refresh:7", hashOf(token));
        verify(redisTemplate).expire("auth:user-refresh:7", SEVEN_DAYS);
    }

    @Test
    void issue_returnsDifferentTokensEachTime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        assertThat(service.issue(1L)).isNotEqualTo(service.issue(1L));
    }

    @Test
    void consume_returnsUserId_deletesTheToken_andRemovesItFromTheIndex() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.getAndDelete(RefreshTokenService.key("abc"))).thenReturn("7");

        assertThat(service.consume("abc")).isEqualTo(7L);
        verify(setOperations).remove("auth:user-refresh:7", hashOf("abc"));
    }

    @Test
    void consume_unknownOrAlreadyUsedToken_throwsInvalidToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.consume("abc"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void consume_blankToken_throwsWithoutCallingRedis() {
        assertThatThrownBy(() -> service.consume(" "))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void revoke_deletesTheTokenAndItsIndexEntry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.getAndDelete(RefreshTokenService.key("abc"))).thenReturn("7");

        service.revoke("abc");

        verify(setOperations).remove("auth:user-refresh:7", hashOf("abc"));
    }

    @Test
    void revoke_unknownToken_isIgnored() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        service.revoke("abc");

        verify(redisTemplate, never()).opsForSet();
    }

    @Test
    void revoke_blankToken_isIgnored() {
        service.revoke(null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void revokeAll_deletesEveryIndexedTokenAndTheIndex() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:user-refresh:7")).thenReturn(Set.of("h1", "h2"));

        service.revokeAll(7L);

        ArgumentCaptor<Collection<String>> keysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(keysCaptor.capture());
        assertThat(keysCaptor.getValue()).containsExactlyInAnyOrder("auth:refresh:h1", "auth:refresh:h2");
        verify(redisTemplate).delete("auth:user-refresh:7");
    }

    @Test
    void revokeAll_withoutTokens_onlyDeletesTheIndex() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:user-refresh:7")).thenReturn(Set.of());

        service.revokeAll(7L);

        verify(redisTemplate, never()).delete(any(Collection.class));
        verify(redisTemplate).delete("auth:user-refresh:7");
    }
}
