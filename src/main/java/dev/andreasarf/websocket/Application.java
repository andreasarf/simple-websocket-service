package dev.andreasarf.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

// ... . -. -.. / -- . / ... --- -- . / -... .. - -.-. --- .. -. / - --- / - .... .. ... / .- -.. -.. .-. . ... ... 1JTuQw94fDLs5Hz6i4xkJwfLmufRUe8kYR
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "dev.andreasarf.websocket.properties")
@EnableFeignClients(basePackages = "dev.andreasarf.websocket.feign")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
