package dev.andreasarf.websocket.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rabbitmq.client.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Value;
import dev.andreasarf.websocket.common.BaseTest;
import dev.andreasarf.websocket.enums.EventType;
import dev.andreasarf.websocket.payload.SystemMessage;
import dev.andreasarf.websocket.payload.ws.NotificationMessage;
import dev.andreasarf.websocket.service.SystemService;
import dev.andreasarf.websocket.util.FileTestUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SystemListenerTest extends BaseTest {

    @Value("${app.notification.queue}")
    private String systemQueue;

    @Value("${app.notification.user.queue}")
    private String userSystemQueue;

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

    @Test
    void givenUserMessage_whenListenForUserNotification_thenShouldPublishMessageToUser() throws Exception {
        // arrange
        final var reply = FileTestUtils.readFileToString("test_files/ai_reply.txt");
        final var conversation = new Conversation(UUID.randomUUID(), UUID.randomUUID(), reply);
        final var userEmail = "john.doe@app.devorg";
        final var systemMessage = SystemMessage.builder()
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .channelUuid(UUID.randomUUID())
                .userEmail(userEmail)
                .data(conversation)
                .build();
        final var topic = "/topic/system.channel." + systemMessage.getChannelUuid();
        final var expected = NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(EventType.SYSTEM_MESSAGE_CREATED)
                .body(objectMapper.convertValue(systemMessage.getData(), new TypeReference<>() {}))
                .build();

        // act
        testTemplate.convertAndSend(userSystemQueue, systemMessage);

        // assert
        verify(simpMessagingTemplate).convertAndSendToUser(eq(userEmail), eq(topic), notificationMessageCaptor.capture());
        assertThat(notificationMessageCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(expected);
        assertThat(notificationMessageCaptor.getValue().getTimestamp())
                .isAfterOrEqualTo(expected.getTimestamp());
    }

    @Test
    void givenUnparsableBody_whenListenForNotification_thenShouldNackWithoutRequeue() throws Exception {
        // arrange
        final var listener = new SystemListener(objectMapper, mock(SystemService.class));
        final var message = messageWithBody("not-json", 1L);
        final var channel = mock(Channel.class);

        // act
        listener.listenForNotification(message, channel);

        // assert
        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(1L, false);
    }

    @Test
    void givenPublishFails_whenListenForNotification_thenShouldNackWithoutRequeue() throws Exception {
        // arrange
        final var systemService = mock(SystemService.class);
        doThrow(new RuntimeException("boom")).when(systemService).publish(any());
        final var listener = new SystemListener(objectMapper, systemService);
        final var systemMessage = SystemMessage.builder()
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .channelUuid(UUID.randomUUID())
                .data("irrelevant")
                .build();
        final var message = messageWithBody(objectMapper.writeValueAsString(systemMessage), 2L);
        final var channel = mock(Channel.class);

        // act
        listener.listenForNotification(message, channel);

        // assert
        verify(channel).basicNack(2L, false, false);
        verify(channel, never()).basicAck(2L, false);
    }

    @Test
    void givenUnparsableBody_whenListenForUserNotification_thenShouldNackWithoutRequeue() throws Exception {
        // arrange
        final var listener = new SystemListener(objectMapper, mock(SystemService.class));
        final var message = messageWithBody("not-json", 3L);
        final var channel = mock(Channel.class);

        // act
        listener.listenForUserNotification(message, channel);

        // assert
        verify(channel).basicNack(3L, false, false);
        verify(channel, never()).basicAck(3L, false);
    }

    @Test
    void givenPublishUserFails_whenListenForUserNotification_thenShouldNackWithoutRequeue() throws Exception {
        // arrange
        final var systemService = mock(SystemService.class);
        doThrow(new RuntimeException("boom")).when(systemService).publishUser(any());
        final var listener = new SystemListener(objectMapper, systemService);
        final var systemMessage = SystemMessage.builder()
                .action(SystemMessage.Action.MESSAGE_CREATED)
                .channelUuid(UUID.randomUUID())
                .userEmail("john.doe@app.devorg")
                .data("irrelevant")
                .build();
        final var message = messageWithBody(objectMapper.writeValueAsString(systemMessage), 4L);
        final var channel = mock(Channel.class);

        // act
        listener.listenForUserNotification(message, channel);

        // assert
        verify(channel).basicNack(4L, false, false);
        verify(channel, never()).basicAck(4L, false);
    }

    private static Message messageWithBody(String body, long deliveryTag) {
        final var properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Data
    @AllArgsConstructor
    private static class Conversation {
        private UUID parentUuid;
        private UUID messageUuid;
        private String message;
    }
}