package dev.andreasarf.websocket.util;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.andreasarf.websocket.common.WebSocketTopics;
import dev.andreasarf.websocket.dto.TopicParameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopicTemplateTest {

    @Test
    void givenArg_whenExpand_thenShouldReturnCompleteTopic() {
        // arrange
        final var channelId = RandomUtils.nextInt(1, 10000000);

        // act
        final var topic = TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, channelId);

        // assert
        assertEquals("/topic/system.channel." + channelId, topic);
    }

    @Test
    void givenValid_whenExtract_thenShouldReturnParameters() {
        // arrange
        final var fullTopic = "/topic/system.channel.123456789";

        // act
        final var extracted = TopicTemplate.extract(fullTopic);

        // assert
        Assertions.assertNotNull(extracted);
        assertEquals(1, extracted.size());
        assertEquals("123456789", extracted.get(TopicParameter.CHANNEL_UUID));
    }

    @Test
    void givenExtra_whenExtract_thenShouldReturnParameters() {
        // arrange
        final var fullTopic = "/topic/system.channel.123456789.item.9876";

        // act
        final var extracted = TopicTemplate.extract(fullTopic);

        // assert
        assertNull(extracted);
    }

    @Test
    void givenLess_whenExtract_thenShouldReturnParameters() {
        // arrange
        final var fullTopic = "/topic/system.channel";

        // act
        final var extracted = TopicTemplate.extract(fullTopic);

        // assert
        assertNull(extracted);
    }
}