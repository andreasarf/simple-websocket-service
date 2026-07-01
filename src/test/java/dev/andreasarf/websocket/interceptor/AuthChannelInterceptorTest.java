package dev.andreasarf.websocket.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import dev.andreasarf.websocket.dto.UserDetailDto;
import dev.andreasarf.websocket.service.AuthorizationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthChannelInterceptorTest {

    @Test
    void givenValidBearerToken_whenPreSend_thenShouldSetUserPrincipal() {
        // arrange
        final var authorizationService = mock(AuthorizationService.class);
        final var userDetail = UserDetailDto.builder().email("john.doe@app.devorg").build();
        when(authorizationService.authenticateSignature("token-123")).thenReturn(userDetail);
        final var interceptor = new AuthChannelInterceptor(authorizationService);
        final var message = connectMessageWithAuthHeader("Bearer token-123");

        // act
        interceptor.preSend(message, mock(MessageChannel.class));

        // assert
        final var accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("john.doe@app.devorg");
    }

    @Test
    void givenNoAuthHeader_whenPreSend_thenShouldNotSetUserPrincipal() {
        // arrange
        final var authorizationService = mock(AuthorizationService.class);
        final var interceptor = new AuthChannelInterceptor(authorizationService);
        final var message = connectMessageWithAuthHeader(null);

        // act
        interceptor.preSend(message, mock(MessageChannel.class));

        // assert
        final var accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isNull();
        verify(authorizationService, never()).authenticateSignature(any());
    }

    @Test
    void givenNonBearerAuthHeader_whenPreSend_thenShouldNotSetUserPrincipal() {
        // arrange
        final var authorizationService = mock(AuthorizationService.class);
        final var interceptor = new AuthChannelInterceptor(authorizationService);
        final var message = connectMessageWithAuthHeader("Basic dXNlcjpwYXNz");

        // act
        interceptor.preSend(message, mock(MessageChannel.class));

        // assert
        final var accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isNull();
        verify(authorizationService, never()).authenticateSignature(any());
    }

    @Test
    void givenAuthenticationThrows_whenPreSend_thenShouldNotSetUserPrincipal() {
        // arrange
        final var authorizationService = mock(AuthorizationService.class);
        when(authorizationService.authenticateSignature(any())).thenThrow(new RuntimeException("boom"));
        final var interceptor = new AuthChannelInterceptor(authorizationService);
        final var message = connectMessageWithAuthHeader("Bearer token-123");

        // act
        interceptor.preSend(message, mock(MessageChannel.class));

        // assert
        final var accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isNull();
    }

    private static Message<byte[]> connectMessageWithAuthHeader(String authHeader) {
        final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
