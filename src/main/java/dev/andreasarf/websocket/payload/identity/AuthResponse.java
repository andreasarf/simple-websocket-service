package dev.andreasarf.websocket.payload.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse implements Serializable {

    private Short tenantId;
    private UUID itemUuid;
    private String email;
}
