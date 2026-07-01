package dev.andreasarf.websocket.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import dev.andreasarf.websocket.enums.EventType;
import dev.andreasarf.websocket.payload.SystemMessage;
import dev.andreasarf.websocket.payload.ws.NotificationMessage;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(SystemServiceImpl.class)
class SystemServiceImplTest {

    @Autowired
    private SystemServiceImpl service;
    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @Captor
    private ArgumentCaptor<NotificationMessage> notificationMessageCaptor;

    @Test
    void givenMessageCreated_whenPublish_thenShouldPublishMessageCreated() {
        testPublish(SystemMessage.Action.MESSAGE_CREATED, EventType.SYSTEM_MESSAGE_CREATED);
    }

    @Test
    void givenMessageUpdated_whenPublish_thenShouldPublishMessageUpdated() {
        testPublish(SystemMessage.Action.MESSAGE_UPDATED, EventType.SYSTEM_MESSAGE_UPDATED);
    }

    @Test
    void givenMessageDeleted_whenPublish_thenShouldPublishMessageDeleted() {
        testPublish(SystemMessage.Action.MESSAGE_DELETED, EventType.SYSTEM_MESSAGE_DELETED);
    }

    @Test
    void givenMessageCreated_whenPublishUser_thenShouldPublishMessageCreatedToUser() {
        testPublishUser(SystemMessage.Action.MESSAGE_CREATED, EventType.SYSTEM_MESSAGE_CREATED);
    }

    @Test
    void givenMessageUpdated_whenPublishUser_thenShouldPublishMessageUpdatedToUser() {
        testPublishUser(SystemMessage.Action.MESSAGE_UPDATED, EventType.SYSTEM_MESSAGE_UPDATED);
    }

    @Test
    void givenMessageDeleted_whenPublishUser_thenShouldPublishMessageDeletedToUser() {
        testPublishUser(SystemMessage.Action.MESSAGE_DELETED, EventType.SYSTEM_MESSAGE_DELETED);
    }

    private void testPublishUser(SystemMessage.Action action, EventType wsEvent) {
        // arrange
        final var system = new Conversation(UUID.randomUUID(), UUID.randomUUID(), "This is new message");
        final var userEmail = "john.doe@app.devorg";
        final var message = SystemMessage.builder()
                .action(action)
                .channelUuid(UUID.randomUUID())
                .userEmail(userEmail)
                .data(system)
                .build();
        final var expected = NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(wsEvent)
                .body(system)
                .build();

        // act
        service.publishUser(message);

        // assert
        verify(simpMessagingTemplate).convertAndSendToUser(eq(userEmail),
                eq("/topic/system.channel." + message.getChannelUuid()),
                notificationMessageCaptor.capture());
        assertThat(notificationMessageCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(expected);
        assertThat(notificationMessageCaptor.getValue().getTimestamp())
                .isAfterOrEqualTo(expected.getTimestamp());
    }

    private void testPublish(SystemMessage.Action action, EventType wsEvent) {
        // arrange
        final var system = new Conversation(UUID.randomUUID(), UUID.randomUUID(), "This is new message");
        final var message = SystemMessage.builder()
                .action(action)
                .channelUuid(UUID.randomUUID())
                .data(system)
                .build();
        final var expected = NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(wsEvent)
                .body(system)
                .build();

        // act
        service.publish(message);

        // assert
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/system.channel." + message.getChannelUuid()),
                notificationMessageCaptor.capture());
        assertThat(notificationMessageCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(expected);
        assertThat(notificationMessageCaptor.getValue().getTimestamp())
                .isAfterOrEqualTo(expected.getTimestamp());
    }

    @Data
    @AllArgsConstructor
    private static class Conversation {
        private UUID parentUuid;
        private UUID messageUuid;
        private String message;
    }
}