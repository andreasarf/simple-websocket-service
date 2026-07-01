package dev.andreasarf.websocket.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import dev.andreasarf.websocket.dto.UserDetailDto;
import dev.andreasarf.websocket.feign.IdentityClient;
import dev.andreasarf.websocket.payload.identity.AuthResponse;
import dev.andreasarf.websocket.payload.identity.UserDataResponse;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AuthorizationServiceImpl.class)
@TestPropertySource(properties = "app.identity.clientSecret=secret")
class AuthorizationServiceImplTest {

    @Autowired
    private AuthorizationServiceImpl service;
    @MockitoBean
    private IdentityClient identityClient;

    @Test
    void givenValidToken_whenAuthenticateSignature_thenShouldReturnUserDetail() {
        // arrange
        final var token = "token-123";
        final var authResponse = new AuthResponse();
        authResponse.setTenantId((short) 461);
        authResponse.setItemUuid(UUID.randomUUID());
        authResponse.setEmail("john.doe@app.devorg");
        when(identityClient.authenticateSignature("Bearer " + token, "secret")).thenReturn(authResponse);

        // act
        final var result = service.authenticateSignature(token);

        // assert
        assertThat(result.getTenantId()).isEqualTo(authResponse.getTenantId());
        assertThat(result.getItemUuid()).isEqualTo(authResponse.getItemUuid());
        assertThat(result.getEmail()).isEqualTo(authResponse.getEmail());
        assertThat(result.getToken()).isEqualTo(token);
    }

    @Test
    void givenUnmatchedTopic_whenIsAuthorized_thenShouldReturnFalse() {
        // arrange
        final var userDetail = userDetail();
        when(identityClient.getUserData(any(), any(), any(), any())).thenReturn(userDataWithChannel(UUID.randomUUID()));

        // act
        final var result = service.isAuthorized(userDetail, "/topic/unknown.pattern");

        // assert
        assertThat(result).isFalse();
    }

    @Test
    void givenMatchingChannelUuid_whenIsAuthorized_thenShouldReturnTrue() {
        // arrange
        final var userDetail = userDetail();
        final var channelUuid = UUID.randomUUID();
        when(identityClient.getUserData(any(), any(), any(), any())).thenReturn(userDataWithChannel(channelUuid));

        // act
        final var result = service.isAuthorized(userDetail, "/topic/system.channel." + channelUuid);

        // assert
        assertThat(result).isTrue();
    }

    @Test
    void givenMismatchedChannelUuid_whenIsAuthorized_thenShouldReturnFalse() {
        // arrange
        final var userDetail = userDetail();
        when(identityClient.getUserData(any(), any(), any(), any())).thenReturn(userDataWithChannel(UUID.randomUUID()));

        // act
        final var result = service.isAuthorized(userDetail, "/topic/system.channel." + UUID.randomUUID());

        // assert
        assertThat(result).isFalse();
    }

    private static UserDetailDto userDetail() {
        return UserDetailDto.builder()
                .tenantId((short) 461)
                .itemUuid(UUID.randomUUID())
                .email("john.doe@app.devorg")
                .build();
    }

    private static UserDataResponse userDataWithChannel(UUID channelUuid) {
        final var item = new UserDataResponse.Item();
        item.setEntityName("CAMPAIGN");
        item.setItemPath(Map.of("Channel", Map.of("id", channelUuid.toString())));
        final var userData = new UserDataResponse();
        userData.setEntityItem(item);
        return userData;
    }
}
