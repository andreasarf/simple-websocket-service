package dev.andreasarf.websocket.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import dev.andreasarf.websocket.dto.UserDetailDto;
import dev.andreasarf.websocket.feign.RbacClient;
import dev.andreasarf.websocket.service.AuthorizationService;
import dev.andreasarf.websocket.util.TopicTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String BEARER = "Bearer ";

    private final RbacClient rbacClient;

    @Value("${app.rbac.clientSecret}")
    private String clientSecret;

    @Override
    public UserDetailDto authenticateSignature(String token) {
        final var authResponse = rbacClient.authenticateSignature(BEARER + token, clientSecret);
        return UserDetailDto.of(authResponse, token);
    }

    @Override
    public boolean isAuthorized(UserDetailDto userDetail, String topic) {
        final var userData = rbacClient.getUserData(userDetail.getTenantId(), userDetail.getEmail(),
                userDetail.getItemUuid(), clientSecret);
        final var parameters = TopicTemplate.extractParams(topic);
        boolean isAuthorized = parameters.isMatched();
        final var reason = new StringBuilder();
        if (isAuthorized) {
            if (parameters.hasChannelUuid()) {
                final var expectedChannelUuid = userData.findEntityId("channel");
                isAuthorized = isAuthorized
                        && StringUtils.equals(expectedChannelUuid, parameters.getChannelUuid().toString());
                reason.append("channel check. result: ").append(isAuthorized).append("\n");
            }
        }

        if (!isAuthorized) {
            log.warn("User {} is not authorized to access topic: {}. Reason: {}. Details: {}", userDetail.getEmail(),
                    topic, reason, userData);
        }

        return isAuthorized;
    }
}
