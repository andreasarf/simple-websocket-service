package dev.andreasarf.websocket.payload.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDataResponse implements Serializable {

    private Short tenantId;
    private Account account;
    private Item entityItem;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account implements Serializable {
        private Integer id;
        private String email;
        private String firstName;
        private String lastName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item implements Serializable {
        private String itemUuid;
        private String entityName;
        private String itemName;
        private Map<String, Object> itemPath;
        private Map<String, Object> itemProperties;
    }

    @JsonIgnore
    public String findEntityId(String idKey) {
        if (entityItem == null) {
            return null;
        }
        return entityItem.getItemPath().entrySet().stream()
                .filter(entry -> StringUtils.equalsIgnoreCase(entry.getKey(), idKey))
                .findFirst()
                .map(entry -> ((Map<String, String>) entry.getValue()))
                .map(map -> map.get("id"))
                .orElseGet(() -> StringUtils.equalsIgnoreCase(entityItem.getEntityName(), idKey)
                        ? entityItem.getItemUuid() : null);
    }
}
