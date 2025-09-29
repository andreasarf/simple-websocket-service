package dev.andreasarf.websocket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Custom interceptor to send receipt for testing purpose
 * It's because simple broker doesn't send receipt for subscribe command
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptChannelInterceptor implements ExecutorChannelInterceptor {

    @Lazy
    private final MessageChannel clientOutboundChannel;

    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel,
                                    MessageHandler handler, Exception ex) {
        // Wrap the message to access STOMP headers
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Only process SUBSCRIBE commands with a receipt header
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) &&
                !StringUtils.isEmpty(accessor.getReceipt())) {

            // Create a RECEIPT frame
            StompHeaderAccessor receiptAccessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
            receiptAccessor.setReceiptId(accessor.getReceipt());
            receiptAccessor.setSessionId(accessor.getSessionId());

            // Send the RECEIPT frame back to the client
            clientOutboundChannel.send(MessageBuilder.createMessage(new byte[0], receiptAccessor.getMessageHeaders()));
        }
    }
}
