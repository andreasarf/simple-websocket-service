package dev.andreasarf.websocket.service;

import dev.andreasarf.websocket.dto.UserDetailDto;

public interface AuthorizationService {
    UserDetailDto authenticateSignature(String token);

    boolean isAuthorized(UserDetailDto userDetail, String topic);
}
