package dev.andreasarf.websocket.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import dev.andreasarf.websocket.common.WebSocketTopics;
import dev.andreasarf.websocket.enums.EventType;
import dev.andreasarf.websocket.payload.SystemMessage;
import dev.andreasarf.websocket.payload.ws.NotificationMessage;
import dev.andreasarf.websocket.service.SystemService;
import dev.andreasarf.websocket.util.TopicTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(SystemMessage message) {
        final var topic = getChannelTopic(message);
        final var notificationMessage = getNotificationMessage(message);
        simpMessagingTemplate.convertAndSend(topic, notificationMessage);
        log.debug("System message published to ws topic: {}", topic);
    }

    private String getChannelTopic(SystemMessage message) {
        return switch (message.getAction()) {
            case MESSAGE_CREATED,
                 MESSAGE_UPDATED,
                 MESSAGE_DELETED -> TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, message.getChannelUuid());
            default -> throw new IllegalArgumentException("Unsupported action: " + message.getAction());
        };
    }

    private NotificationMessage getNotificationMessage(SystemMessage message) {
        final var event = getEventType(message);
        return NotificationMessage.of(message, event);
    }

    private static EventType getEventType(SystemMessage message) {
        return switch (message.getAction()) {
            case MESSAGE_CREATED -> EventType.SYSTEM_MESSAGE_CREATED;
            case MESSAGE_UPDATED -> EventType.SYSTEM_MESSAGE_UPDATED;
            case MESSAGE_DELETED -> EventType.SYSTEM_MESSAGE_DELETED;
            default -> throw new IllegalArgumentException("Unsupported action: " + message.getAction());
        };
    }
}
