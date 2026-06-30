# Simple Websocket Service

A websocket service with STOMP protocol enhanced with Authentication and Authorization layer.

## Tech Stacks

1. Java 21 + Maven
2. Spring Boot 3.5.x
    - Spring Websocket (STOMP server)
3. RabbitMQ via Docker
    - Queue broker
    - STOMP broker
4. Docker

## Topic Naming Convention

Since we use RabbitMQ as the broker, we need to follow the topic naming convention.

1. Name cannot contain slash `/` like in the URL (RabbitMQ forbids it), except the prefixes.
2. Instead, we use dot `.` to separate the topic hierarchy.
    - e.g. `/topic/system.channel.123`
3. It has prefix names:
    - `/topic`: as broadcasting publisher
    - `/user/topic`: as user-specific publisher
    - `/app`: as send entrypoint

## Start and Connect

### Prerequisites
1. Start RabbitMQ server
    ```bash
    docker-compose up -d
    ```
2. Start Mock server for Identity service
    - Can use Mockoon, the config file can be found in [identity.json](mockoon/identity.json)

### Start Server

1. Run RabbitMQ server
2. Run `Application.java`

#### Options

1. By default, the STOMP server will use simple broker.
    - To use RabbitMQ, set `app.websocket.broker.localtest.useSimple` to `true`

### Connect as Client

1. Prepare the STOMP/SockJS client.
2. Connect to `ws://localhost:8881/ws-connect` (adjust host and port if needed)
3. Subscribe to one of the topics.

## Security

1. Use token from Identity service to authenticate on `CONNECT` command. It, then, save the session for subsequent commands.
    - Put `Authorization: Bearer <token>` on STOMP header.
2. On `SUBSCRIBE` command, it will authorize by checking topic/destination URI against itemPath from Identity service.
3. In general, this layer can toggled by properties `app.websocket.auth.enabled`.

## Architecture Design

```mermaid
sequenceDiagram
    participant web as WebApp
    participant WsBroker@{ "type" : "queue" }
    participant ws as websocket-router
    participant identity as identity-service
    participant SystemQ@{ "type" : "queue" }
    participant system as system-api
    
    rect rgba(0, 0, 255, .1)
        Note over web: connect
        web ->> ws : Connect with Authorization header
        activate ws
        ws ->> ws : Extract token
        ws ->> identity : Verify auth token
        
        alt token ok
            ws ->> ws : Save session
            ws ->> web : Accept connection
        else token nok
            ws -x web : Reject connection
        end
    end
    
    rect rgba(0, 255, 255, .1)
        Note over web: subscribe
        web ->> ws : Subscribe to a topic
        ws ->> identity : Get user data
        ws ->> ws : Check authorization
        
        alt authz ok
            ws ->> web : Accept subscription
        else auth nok
            ws -x web : Reject subscription
        end
    end
    
    rect rgba(255, 0, 255, .1)
        Note over web: publish notification
        activate system
        system ->> system : Do some operation
        system ->> SystemQ : Publish notification
        activate SystemQ
        deactivate system
        SystemQ ->> ws : Listen notification
        deactivate SystemQ
        ws ->> ws : Build notification
        ws ->> WsBroker : Publish WebSocket message
        activate WsBroker
        WsBroker ->> web : Receive message
        deactivate WsBroker
        web ->> web : Update data
        deactivate ws
    end
```

## Testing

We have a test client included in `resources/test.html`. It is based on JavaScript.

1. Open [test.html](src/main/resources/test.html) in browser.
2. Open developer console. Observe the log messages.
3. Add or delete topic subscriptions in the script.
4. Send Rabbit message from the RabbitMQ management console to trigger some topics.

NOTE: By default, it connects to HelloController and subscribe to it.
Also, it subscribes to an additional topic triggered by RabbitMQ queue.