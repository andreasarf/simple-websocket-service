package dev.andreasarf.websocket.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {

    private Cors cors = new Cors();
    private Map<String, Broker> broker = new HashMap<>();
    private Auth auth = new Auth();

    public Broker getLocalTest() {
        return broker.get("localtest");
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of();

        public String[] getAllowedOriginsArray() {
            return allowedOrigins.toArray(new String[0]);
        }
    }

    @Data
    public static class Broker {
        private Boolean useSimple = true;
    }

    @Data
    public static class Auth {
        private Boolean enabled = true;
    }

}
