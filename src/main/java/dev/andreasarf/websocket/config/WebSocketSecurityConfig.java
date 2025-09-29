package dev.andreasarf.websocket.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import dev.andreasarf.websocket.interceptor.WebSocketAuthManager;
import dev.andreasarf.websocket.properties.WebSocketProperties;

@Slf4j
@Configuration
@EnableWebSocketSecurity
@RequiredArgsConstructor
public class WebSocketSecurityConfig {

    private final WebSocketProperties wsProperties;
    private final WebSocketAuthManager authManager;

    @PostConstruct
    public void init() {
        if (!wsProperties.getAuth().getEnabled()) {
            log.warn("!!!WebSocket authorization is disabled!!! All WebSocket traffic will be permitted.");
        }
    }

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        if (!wsProperties.getAuth().getEnabled()) {
            return messages
                    .anyMessage().permitAll()
                    .build();
        }

        return messages
                .simpTypeMatchers(SimpMessageType.CONNECT).permitAll() // auth is handled by the AuthChannelInterceptor
                .simpDestMatchers("/app/hello").permitAll()
                .simpSubscribeDestMatchers("/topic/greetings").permitAll()
                .simpSubscribeDestMatchers("/topic/**").access(authManager)
                .anyMessage().authenticated()
                .build();
    }

    /**
     * This bean overrides Spring Security's default WebSocket CSRF protection,
     * which is the XorCsrfChannelInterceptor that cannot be disabled.
     */
    @Bean
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
            // This is a "do-nothing" interceptor.
            // It will be used instead of the default CSRF-checking interceptor,
            // effectively disabling the WebSocket-level CSRF check.
        };
    }

}
