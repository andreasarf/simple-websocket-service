package dev.andreasarf.websocket.service;

import dev.andreasarf.websocket.payload.SystemMessage;

public interface SystemService {
    void publish(SystemMessage message);

    void publishUser(SystemMessage message);
}
