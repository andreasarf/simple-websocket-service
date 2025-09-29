package dev.andreasarf.websocket.dto;

import lombok.Builder;
import lombok.Data;
import dev.andreasarf.websocket.payload.rbac.AuthResponse;

import java.util.UUID;

@Data
@Builder
public class UserDetailDto {

    private Short tenantId;
    private UUID itemUuid;
    private String email;
    private String token;

    public static UserDetailDto of(AuthResponse response, String token) {
        return UserDetailDto.builder()
                .tenantId(response.getTenantId())
                .itemUuid(response.getItemUuid())
                .email(response.getEmail())
                .token(token)
                .build();
    }
}
