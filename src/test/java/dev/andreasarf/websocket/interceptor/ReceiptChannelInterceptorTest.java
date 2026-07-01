package dev.andreasarf.websocket.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReceiptChannelInterceptorTest {

    @Test
    void givenSubscribeWithReceipt_whenAfterMessageHandled_thenShouldSendReceiptFrame() {
        // arrange
        final var clientOutboundChannel = mock(MessageChannel.class);
        final var interceptor = new ReceiptChannelInterceptor(clientOutboundChannel);
        final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setReceipt("receipt-1");
        accessor.setSessionId("session-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // act
        interceptor.afterMessageHandled(message, mock(MessageChannel.class), null, null);

        // assert
        verify(clientOutboundChannel).send(any());
    }

    @Test
    void givenSubscribeWithoutReceipt_whenAfterMessageHandled_thenShouldNotSendReceiptFrame() {
        // arrange
        final var clientOutboundChannel = mock(MessageChannel.class);
        final var interceptor = new ReceiptChannelInterceptor(clientOutboundChannel);
        final var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // act
        interceptor.afterMessageHandled(message, mock(MessageChannel.class), null, null);

        // assert
        verify(clientOutboundChannel, never()).send(any());
    }

    @Test
    void givenNonSubscribeCommand_whenAfterMessageHandled_thenShouldNotSendReceiptFrame() {
        // arrange
        final var clientOutboundChannel = mock(MessageChannel.class);
        final var interceptor = new ReceiptChannelInterceptor(clientOutboundChannel);
        final var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setReceipt("receipt-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // act
        interceptor.afterMessageHandled(message, mock(MessageChannel.class), null, null);

        // assert
        verify(clientOutboundChannel, never()).send(any());
    }
}
