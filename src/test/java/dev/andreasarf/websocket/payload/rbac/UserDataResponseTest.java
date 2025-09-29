package dev.andreasarf.websocket.payload.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import dev.andreasarf.websocket.util.FileTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserDataResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenIdExists_whenFindEntityId_thenShouldReturnId() throws Exception {
        // arrange
        final var itemPathJson = FileTestUtils.readFileToString("test_files/user_data_response.json");
        final var userData = objectMapper.readValue(itemPathJson, UserDataResponse.class);

        // act
        final var actualId = userData.findEntityId("channel");

        // assert
        assertEquals("0698f4ff-211e-4849-97c6-7ac9e11c0c9e", actualId);
    }

    @Test
    void givenIdNotExists_whenFindEntityId_thenShouldReturnNull() throws Exception {
        // arrange
        final var itemPathJson = FileTestUtils.readFileToString("test_files/user_data_response.json");
        final var userData = objectMapper.readValue(itemPathJson, UserDataResponse.class);

        // act
        final var actualId = userData.findEntityId("Option");

        // assert
        assertNull(actualId);
    }
}