package dev.andreasarf.websocket.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemMessage implements Serializable {

    private Action action;
    private UUID channelUuid;
    private Object data;

    public enum Action {
        MESSAGE_CREATED,
        MESSAGE_UPDATED,
        MESSAGE_DELETED,
    }
}
