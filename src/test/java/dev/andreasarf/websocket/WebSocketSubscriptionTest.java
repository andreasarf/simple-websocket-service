package dev.andreasarf.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import dev.andreasarf.websocket.common.BaseWebSocketTest;
import dev.andreasarf.websocket.common.WebSocketTopics;
import dev.andreasarf.websocket.enums.EventType;
import dev.andreasarf.websocket.payload.SystemMessage;
import dev.andreasarf.websocket.payload.ws.NotificationMessage;
import dev.andreasarf.websocket.util.TopicTemplate;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSocketSubscriptionTest extends BaseWebSocketTest {

    @Test
    void givenValidAuthAndInItemPath_whenSubscribeTopic_thenShouldPublishNotification() throws Exception {
        // arrange
        final var channelUuid = CHANNEL_UUID;
        final var topic = TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, channelUuid);
        final var connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        final var authResponse = arrangeIdentityAuth();
        final var userData = arrangeIdentityUserData(authResponse);
        final var system = SystemMessage.builder()
                .channelUuid(channelUuid)
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .data(new Message("Test message. This is the reply."))
                .build();
        final var expected = NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(EventType.SYSTEM_MESSAGE_CREATED)
                .body(objectMapper.convertValue(system.getData(), Map.class))
                .build();

        // act
        // connect
        stompSession = stompClient.connectAsync(WS_URI, (WebSocketHttpHeaders) null, connectHeaders,
                        sessionHandler())
                .get(1, TimeUnit.SECONDS);
        stompSession.setAutoReceipt(true);

        // subscribe
        stompSession.subscribe(topic, sessionFrameHandler())
                .addReceiptTask(() -> receiptFuture.complete(null));

        receiptFuture.get(1, TimeUnit.SECONDS);

        // send/trigger event/notification via rabbitMQ
        testTemplate.convertAndSend(systemQueueName, system);

        // assert
        identityWireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IDENTITY_AUTH_PATH)));
        identityWireMockServer.verify(1, getRequestedFor(urlPathTemplate(IDENTITY_USER_DATA_PATH + "{tenantId}")));
        verify(simpMessagingTemplate).convertAndSend(eq(topic), any(NotificationMessage.class));
        final var actualNotification = (NotificationMessage) blockingQueue.poll(1, TimeUnit.SECONDS);
        assertThat(actualNotification)
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(expected);
        assertThat(actualNotification.getTimestamp())
                .isAfterOrEqualTo(expected.getTimestamp());
    }

    @Test
    void givenValidAuthAndNotInItemPath_whenSubscribeTopic_thenShouldPublishNotification() throws Exception {
        // arrange
        final var channelUuid = CHANNEL_UUID_2;
        final var topic = TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, channelUuid);
        final var connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        final var authResponse = arrangeIdentityAuth();
        final var userData = arrangeIdentityUserData(authResponse, "test_files/user_data_2_response.json");
        final var system = SystemMessage.builder()
                .channelUuid(channelUuid)
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .data(new Message("Test message. This is the reply."))
                .build();
        final var expected = NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(EventType.SYSTEM_MESSAGE_CREATED)
                .body(objectMapper.convertValue(system.getData(), Map.class))
                .build();

        // act
        // connect
        stompSession = stompClient.connectAsync(WS_URI, (WebSocketHttpHeaders) null, connectHeaders,
                        sessionHandler())
                .get(1, TimeUnit.SECONDS);
        stompSession.setAutoReceipt(true);

        // subscribe
        stompSession.subscribe(topic, sessionFrameHandler())
                .addReceiptTask(() -> receiptFuture.complete(null));

        receiptFuture.get(1, TimeUnit.SECONDS);

        // send/trigger event/notification via rabbitMQ
        testTemplate.convertAndSend(systemQueueName, system);

        // assert
        identityWireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IDENTITY_AUTH_PATH)));
        identityWireMockServer.verify(1, getRequestedFor(urlPathTemplate(IDENTITY_USER_DATA_PATH + "{tenantId}")));
        verify(simpMessagingTemplate).convertAndSend(eq(topic), any(NotificationMessage.class));
        final var actualNotification = (NotificationMessage) blockingQueue.poll(1, TimeUnit.SECONDS);
        assertThat(actualNotification)
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(expected);
        assertThat(actualNotification.getTimestamp())
                .isAfterOrEqualTo(expected.getTimestamp());
    }

    @Test
    void givenRejectedAuth_whenSubscribeTopic_thenShouldNotPublishNotification() throws Exception {
        // arrange
        final var token = "bogus-token"; // problematic component; auth is failed
        final var channelUuid = CHANNEL_UUID;
        final var topic = TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, channelUuid);
        final var connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        final var authResponse = arrangeIdentityAuth();
        final var userData = arrangeIdentityUserData(authResponse);
        final var system = SystemMessage.builder()
                .channelUuid(channelUuid)
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .data(new Message("Test message. This is the reply."))
                .build();

        // act
        // connect
        stompSession = stompClient.connectAsync(WS_URI, (WebSocketHttpHeaders) null, connectHeaders,
                        sessionHandler())
                .get(1, TimeUnit.SECONDS);

        // subscribe
        stompSession.subscribe(topic, sessionFrameHandler());

        errorFuture.get(1, TimeUnit.SECONDS);

        // send/trigger event/notification via rabbitMQ
        testTemplate.convertAndSend(systemQueueName, system);

        // assert
        identityWireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IDENTITY_AUTH_PATH)));
        identityWireMockServer.verify(0, getRequestedFor(urlPathTemplate(IDENTITY_USER_DATA_PATH + "{tenantId}")));
        verify(simpMessagingTemplate).convertAndSend(eq(topic), any(NotificationMessage.class));
        final var actualNotification = (NotificationMessage) blockingQueue.poll(1, TimeUnit.SECONDS);
        assertNull(actualNotification);
    }

    @Test
    void givenRejectedAuthz_whenSubscribeTopic_thenShouldNotPublishNotification() throws Exception {
        // arrange
        final var channelUuid = UUID.randomUUID(); // problematic component; client doesn't have authority to access said channelUuid
        final var topic = TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, channelUuid);
        final var connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        final var authResponse = arrangeIdentityAuth();
        final var userData = arrangeIdentityUserData(authResponse);
        final var system = SystemMessage.builder()
                .channelUuid(channelUuid)
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .data(new Message("Test message. This is the reply."))
                .build();

        // act
        // connect
        stompSession = stompClient.connectAsync(WS_URI, (WebSocketHttpHeaders) null, connectHeaders,
                        sessionHandler())
                .get(1, TimeUnit.SECONDS);

        // subscribe
        stompSession.subscribe(topic, sessionFrameHandler());

        errorFuture.get(1, TimeUnit.SECONDS);

        // send/trigger event/notification via rabbitMQ
        testTemplate.convertAndSend(systemQueueName, system);

        // assert
        identityWireMockServer.verify(1, postRequestedFor(urlPathEqualTo(IDENTITY_AUTH_PATH)));
        identityWireMockServer.verify(1, getRequestedFor(urlPathTemplate(IDENTITY_USER_DATA_PATH + "{tenantId}")));
        verify(simpMessagingTemplate).convertAndSend(eq(topic), any(NotificationMessage.class));
        final var actualNotification = (NotificationMessage) blockingQueue.poll(1, TimeUnit.SECONDS);
        assertNull(actualNotification);
    }

    @Data
    @AllArgsConstructor
    private static class Message implements Serializable {
        private String message;
    }

    private StompSessionHandlerAdapter sessionHandler() {
        return new StompSessionHandlerAdapter() {

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                log.error("HANDLE EXCEPTION: {}", exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.error("TRANSPORT ERROR: {}", exception.getMessage());
                errorFuture.complete(null);
            }
        };
    }

    private StompFrameHandler sessionFrameHandler() {
        return new StompFrameHandler() {

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // receive subscription event/message here and put it in a blocking queue
                blockingQueue.add(payload);
            }
        };
    }
}
