package dev.andreasarf.websocket.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Value;
import dev.andreasarf.websocket.common.BaseTest;
import dev.andreasarf.websocket.enums.EventType;
import dev.andreasarf.websocket.payload.SystemMessage;
import dev.andreasarf.websocket.payload.ws.NotificationMessage;
import dev.andreasarf.websocket.util.FileTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class SystemListenerTest extends BaseTest {

    @Value("${app.notification.queue}")
    private String systemQueue;

    @Captor
    private ArgumentCaptor<NotificationMessage> notificationMessageCaptor;

    @Test
    void givenReply_whenListenForReply_thenShouldPublishReplyMessage() throws Exception {
        // arrange
        final var reply = FileTestUtils.readFileToString("test_files/ai_reply.txt");
        final var conversation = new Conversation(UUID.randomUUID(), UUID.randomUUID(), reply);
        final var systemMessage = SystemMessage.builder()
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .channelUuid(UUID.randomUUID())
                .data(conversation)
                .build();
        final var topic = "/topic/system.channel." + systemMessage.getChannelUuid();
        final var expected = NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(EventType.SYSTEM_MESSAGE_CREATED)
                .body(objectMapper.convertValue(systemMessage.getData(), new TypeReference<>() {}))
                .build();

        // act
        testTemplate.convertAndSend(systemQueue, systemMessage);

        // assert
        verify(simpMessagingTemplate).convertAndSend(eq(topic), notificationMessageCaptor.capture());
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