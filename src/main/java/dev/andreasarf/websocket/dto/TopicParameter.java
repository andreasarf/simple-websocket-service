package dev.andreasarf.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TopicParameter {

    public static final String CHANNEL_UUID = "channelUuid";

    private Map<String, String> parameters;

    public boolean isMatched() {
        return parameters != null && !parameters.isEmpty();
    }

    public boolean hasChannelUuid() {
        return parameters.containsKey(CHANNEL_UUID);
    }

    public UUID getChannelUuid() {
        return UUID.fromString(parameters.get(CHANNEL_UUID));
    }
}
