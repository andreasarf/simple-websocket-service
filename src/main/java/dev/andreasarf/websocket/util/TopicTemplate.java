package dev.andreasarf.websocket.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriTemplate;
import dev.andreasarf.websocket.common.WebSocketTopics;
import dev.andreasarf.websocket.dto.TopicParameter;

import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class TopicTemplate {

    private static final List<UriTemplate> uriTemplates;

    static {
        uriTemplates = WebSocketTopics.ALL_TOPICS.stream()
                .map(UriTemplate::new)
                .toList();
    }

    /**
     * Use Spring UriTemplate to expand topic with arguments.
     *
     * @param topic topic format
     * @param args  list of arguments to be expanded in the topic
     * @return complete topic with arguments expanded
     */
    public static String expand(String topic, Object... args) {
        final var uriTemplate = new UriTemplate(topic);
        return uriTemplate.expand(args).toString();
    }

    public static Map<String, String> extract(String fullTopic) {
        for (UriTemplate uriTemplate : uriTemplates) {
            final var uriVariables = uriTemplate.match(fullTopic);
            final var containDots = uriVariables.values().stream().anyMatch(val -> val.contains("."));
            if (!uriVariables.isEmpty() && !containDots) {
                return uriVariables;
            }
        }
        return null;
    }

    public static TopicParameter extractParams(String fullTopic) {
        return new TopicParameter(extract(fullTopic));
    }
}
