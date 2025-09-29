package dev.andreasarf.websocket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import dev.andreasarf.websocket.interceptor.AuthChannelInterceptor;
import dev.andreasarf.websocket.interceptor.ReceiptChannelInterceptor;
import dev.andreasarf.websocket.properties.WebSocketProperties;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Profile("!test")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final Environment environment;
    private final RabbitProperties rabbitProperties;
    private final WebSocketProperties wsProperties;
    private final AuthChannelInterceptor authChannelInterceptor;
    private final ReceiptChannelInterceptor receiptChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        if (wsProperties.getLocalTest().getUseSimple()) {
            registry.enableSimpleBroker("/topic");
        } else {
            registry.enableStompBrokerRelay("/topic/")
                    .setRelayHost(rabbitProperties.getHost());
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint clients will use to connect to the WebSocket server.
        // withSockJS() provides a fallback for browsers that don't support WebSocket.
        registry.addEndpoint("/ws-connect")
                .setAllowedOriginPatterns(wsProperties.getCors().getAllowedOriginsArray())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        if (wsProperties.getAuth().getEnabled()) {
            registration.interceptors(authChannelInterceptor);
        }
        if (environment.acceptsProfiles(Profiles.of("wstest"))) {
            registration.interceptors(receiptChannelInterceptor);
        }
    }
}
