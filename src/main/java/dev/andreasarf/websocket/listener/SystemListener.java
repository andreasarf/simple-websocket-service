package dev.andreasarf.websocket.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import dev.andreasarf.websocket.payload.SystemMessage;
import dev.andreasarf.websocket.service.SystemService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemListener {

    private final ObjectMapper objectMapper;
    private final SystemService systemService;

    @RabbitListener(queues = "${app.notification.queue}")
    public void listenForReply(Message message, Channel channel) throws Exception {
        String body = new String(message.getBody());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("Received system properties: {}, body: {}", message.getMessageProperties(), body);
        try {
            final var systemMessage = objectMapper.readValue(body, SystemMessage.class);
            systemService.publish(systemMessage);
            channel.basicAck(deliveryTag, false);
            log.info("Acked system: {}", message);
        } catch (JsonProcessingException e) {
            log.error("Failed parse, body: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("Failed to process system, body: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
