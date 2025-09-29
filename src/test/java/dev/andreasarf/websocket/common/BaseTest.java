package dev.andreasarf.websocket.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.test.TestRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import dev.andreasarf.websocket.config.RabbitTestConfig;

@SpringBootTest
@Import(RabbitTestConfig.class)
@EnableAutoConfiguration(exclude = {
        RabbitAutoConfiguration.class
})
@ActiveProfiles("test")
public abstract class BaseTest {

    @Autowired
    @Qualifier("testTemplate")
    protected TestRabbitTemplate testTemplate;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected SimpMessagingTemplate simpMessagingTemplate;
}
