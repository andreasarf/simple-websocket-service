# Topic Routing

## Naming Rules

RabbitMQ forbids `/` in routing keys and exchange names (except as STOMP broker prefix). All topic hierarchy uses `.` as separator:

```
/topic/system.channel.{channelUuid}
```

- `/topic` prefix → STOMP broker relay prefix (RabbitMQ maps this to a topic exchange)
- `/user/topic` prefix → user-specific delivery; Spring prepends `/user/<email>` internally
- `/app` prefix → inbound send endpoint (routes to `@MessageMapping` controllers)

All canonical topic patterns are registered in `WebSocketTopics.ALL_TOPICS` (a `Set<String>`). This set drives `TopicTemplate`'s static `UriTemplate` list at startup.

---

## TopicTemplate

**File:** `util/TopicTemplate.java`

Two main operations:

### expand — build a full topic from a pattern + args
```java
TopicTemplate.expand(WebSocketTopics.CHANNEL_TOPIC, channelUuid)
// → "/topic/system.channel.0698f4ff-211e-4849-97c6-7ac9e11c0c9e"
```
Uses Spring's `UriTemplate.expand()`. Args are positional.

### extractParams — parse a full topic back to named parameters
```java
TopicParameter params = TopicTemplate.extractParams(fullTopic);
params.isMatched();       // false if topic matches no known pattern
params.hasChannelUuid();  // true if {channelUuid} was extracted
params.getChannelUuid();  // UUID parsed from the extracted string
```
Iterates all `UriTemplate`s in `ALL_TOPICS`. Skips matches where any extracted value contains a `.` — this prevents partial matches where a dot in a UUID segment causes a false positive across hierarchy boundaries.

**Non-obvious:** `extract()` returns `null` (not empty map) when no pattern matches. `TopicParameter` wraps this null and `isMatched()` returns `false`, which short-circuits authorization in `AuthorizationServiceImpl`.

---

## Inbound RabbitMQ → WebSocket Delivery

**Files:** `listener/SystemListener.java`, `service/impl/SystemServiceImpl.java`

Two queues, two delivery modes:

| Queue (`app.*`) | Listener method | Service method | Delivery |
|---|---|---|---|
| `app.notification` | `listenForNotification` | `publish()` | `convertAndSend(topic, msg)` — broadcast to all subscribers |
| `app.user.notification` | `listenForUserNotification` | `publishUser()` | `convertAndSendToUser(email, topic, msg)` — user-specific |

`SystemMessage` shape (inbound from RabbitMQ):
```json
{ "action": "MESSAGE_CREATED", "channelUuid": "...", "userEmail": "...", "data": {} }
```

`action` drives both the topic (via `getChannelTopic`) and the `EventType` in the outbound `NotificationMessage`. Currently only `MESSAGE_CREATED/UPDATED/DELETED` are handled — all map to `CHANNEL_TOPIC`. Adding a new action requires extending both switch expressions in `SystemServiceImpl`.

`NotificationMessage` shape (outbound to WebSocket client):
```json
{ "timestamp": "...", "event": "SYSTEM_MESSAGE_CREATED", "body": {} }
```

---

## Acknowledgement

`RabbitMQConfig` sets `AcknowledgeMode.MANUAL` on the listener container factory. Listeners manually `basicAck` on success and `basicNack(requeue=false)` on failure — dead-letter without requeue. Parse failures (`JsonProcessingException`) and processing failures are both nacked without requeue to avoid poison-message loops.
