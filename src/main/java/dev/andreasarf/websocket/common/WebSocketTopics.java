package dev.andreasarf.websocket.common;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class WebSocketTopics {

    public static final String BASE_TOPIC = "/topic";

    // system
    public static final String SYSTEM_TOPIC = BASE_TOPIC + "/system";
    public static final String CHANNEL_TOPIC = SYSTEM_TOPIC + ".channel.{channelUuid}";

    // all topics
    public static final Set<String> ALL_TOPICS = Set.of(
            CHANNEL_TOPIC
    );
}
