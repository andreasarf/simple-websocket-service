package dev.andreasarf.websocket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;
import org.springframework.stereotype.Component;
import dev.andreasarf.websocket.dto.UserDetailDto;
import dev.andreasarf.websocket.service.AuthorizationService;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthManager implements AuthorizationManager<MessageAuthorizationContext<?>> {

    private final AuthorizationService authorizationService;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, MessageAuthorizationContext<?> object) {
        final var accessor = StompHeaderAccessor.wrap(object.getMessage());
        final var topic = accessor.getDestination();
        try {
            final var authToken = (UsernamePasswordAuthenticationToken) accessor.getUser();
            final var userDetail = (UserDetailDto) authToken.getPrincipal();

            boolean isAllowed = authorizationService.isAuthorized(userDetail, topic);

            // Return a decision based on the boolean result
            return new AuthorizationDecision(isAllowed);
        } catch (Exception e) {
            log.warn("Failed to authorize WebSocket message to {}", topic, e);
            return new AuthorizationDecision(false);
        }
    }

}
