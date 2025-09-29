package dev.andreasarf.websocket.payload.ws;

import lombok.Builder;
import lombok.Data;
import dev.andreasarf.websocket.enums.EventType;
import dev.andreasarf.websocket.payload.SystemMessage;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
public class NotificationMessage implements Serializable {

    private Instant timestamp;
    private EventType event;
    private Object body;

    public static NotificationMessage of(SystemMessage message, EventType eventType) {
        return NotificationMessage.builder()
                .timestamp(Instant.now())
                .event(eventType)
                .body(message.getData())
                .build();
    }
}
