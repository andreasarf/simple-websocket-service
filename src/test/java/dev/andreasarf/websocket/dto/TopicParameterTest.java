package dev.andreasarf.websocket.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TopicParameterTest {

    @Test
    void givenNullParameters_whenIsMatched_thenShouldReturnFalse() {
        // arrange
        final var parameter = new TopicParameter(null);

        // act & assert
        assertThat(parameter.isMatched()).isFalse();
    }

    @Test
    void givenEmptyParameters_whenIsMatched_thenShouldReturnFalse() {
        // arrange
        final var parameter = new TopicParameter(Map.of());

        // act & assert
        assertThat(parameter.isMatched()).isFalse();
    }

    @Test
    void givenNonEmptyParameters_whenIsMatched_thenShouldReturnTrue() {
        // arrange
        final var parameter = new TopicParameter(Map.of(TopicParameter.CHANNEL_UUID, UUID.randomUUID().toString()));

        // act & assert
        assertThat(parameter.isMatched()).isTrue();
    }

    @Test
    void givenChannelUuidParameter_whenHasChannelUuid_thenShouldReturnTrue() {
        // arrange
        final var channelUuid = UUID.randomUUID();
        final var parameter = new TopicParameter(Map.of(TopicParameter.CHANNEL_UUID, channelUuid.toString()));

        // act & assert
        assertThat(parameter.hasChannelUuid()).isTrue();
        assertThat(parameter.getChannelUuid()).isEqualTo(channelUuid);
    }

    @Test
    void givenNoChannelUuidParameter_whenHasChannelUuid_thenShouldReturnFalse() {
        // arrange
        final var parameter = new TopicParameter(Map.of("other", "value"));

        // act & assert
        assertThat(parameter.hasChannelUuid()).isFalse();
    }
}
