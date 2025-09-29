package dev.andreasarf.websocket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import dev.andreasarf.websocket.dto.UserDetailDto;
import dev.andreasarf.websocket.service.AuthorizationService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final AuthorizationService authorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("[AuthInterceptor] Received CONNECT message");
            final var userDetailOpt = authenticate(accessor);

            if (userDetailOpt.isPresent()) {
                final var userDetail = userDetailOpt.get();
                final var auth = new UsernamePasswordAuthenticationToken(userDetail, null,
                        List.of(new SimpleGrantedAuthority("USER")));
                accessor.setUser(auth);
            }
        }

        return message;
    }

    private Optional<UserDetailDto> authenticate(StompHeaderAccessor accessor) {
        try {
            String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return Optional.of(authorizationService.authenticateSignature(token));
            }

        } catch (Exception e) {
            log.debug("Failed to authenticate signature", e);
        }

        return Optional.empty();
    }
}
