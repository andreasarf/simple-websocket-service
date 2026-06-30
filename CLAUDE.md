# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run tests (all, parallel with 5 threads)
./mvnw test

# Run a single test class
./mvnw test -Dtest=TopicTemplateTest

# Run the app (requires RabbitMQ and Identity mock up)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Start RabbitMQ
docker-compose up -d
```

## Deep context

For detailed per-topic context, see `docs/claude/`:
- [auth-authz-flow.md](docs/claude/auth-authz-flow.md) — CONNECT auth and SUBSCRIBE authz, CSRF bypass, identity service wiring
- [topic-routing.md](docs/claude/topic-routing.md) — naming rules, `TopicTemplate` mechanics, RabbitMQ → WebSocket delivery
- [test-infrastructure.md](docs/claude/test-infrastructure.md) — two base classes, `ReceiptChannelInterceptor`, WireMock and Rabbit test config

## Architecture

This is a Spring Boot WebSocket relay service. Its job is to authenticate STOMP clients, authorize their subscriptions, and forward RabbitMQ messages to the right WebSocket clients.

### Request flow

**Client connect → subscribe → receive messages:**

1. **CONNECT**: `AuthChannelInterceptor` extracts `Authorization: Bearer <token>` from the STOMP header, calls the Identity service (`IdentityClient`) to verify it, and stores a `UserDetailDto` as the Spring Security principal on the session.
2. **SUBSCRIBE**: `WebSocketAuthManager` calls `AuthorizationService.isAuthorized()`, which fetches user data from Identity and validates that the subscribed topic's `channelUuid` matches what the user has access to.
3. **Notification delivery**: `SystemListener` consumes from two RabbitMQ queues:
   - `app.notification` → `SystemService.publish()` → broadcasts to `/topic/...`
   - `app.user.notification` → `SystemService.publishUser()` → sends to `/user/topic/...` (user-specific, keyed by email)

### Topic naming

Topics use dot-separated hierarchy (no slashes except the prefix) because RabbitMQ forbids slashes in exchange/routing key names:
- Pattern: `/topic/system.channel.{channelUuid}`
- All topic patterns are registered in `WebSocketTopics.ALL_TOPICS`
- `TopicTemplate.expand()` fills in UUIDs; `TopicTemplate.extractParams()` parses a full topic back to `TopicParameter`

### Broker configuration

`WebSocketConfig` switches between two broker modes via `app.websocket.broker.localtest.useSimple`:
- `false` (default, production): STOMP broker relay to RabbitMQ on port 5672
- `true` (local dev/test): in-memory simple broker

The `WebSocketConfig` bean is excluded from the `test` profile; tests use `application-test.yaml` which forces `useSimple: true`.

### Key config properties (`app.*`)

| Property | Purpose |
|---|---|
| `app.websocket.auth.enabled` | Toggle entire auth/authz layer |
| `app.websocket.broker.localtest.useSimple` | Use in-memory broker vs RabbitMQ relay |
| `app.websocket.cors.allowedOrigins` | CORS origins for the WebSocket endpoint |
| `app.notification.queue` | RabbitMQ queue for broadcast notifications |
| `app.notification.user.queue` | RabbitMQ queue for user-specific notifications |
| `app.identity.url` | Base URL of the Identity service |
| `app.identity.clientSecret` | Shared secret sent as `X-Internal-Secret` header |

### Test conventions

- Section comments in every test method: `// arrange`, `// act`, `// assert` (or `// act & assert`)
- Method naming: `given{Condition}_when{Action}_then[Subject]Should{Expectation}` — `Should` is mandatory, subject is optional (e.g. `thenShouldSkip`, `thenLockShouldBeReleased`)
- Group related tests into `@Nested` inner classes by field or concern; inner class names must be suffixed with `Test`

### Test structure

- **`BaseTest`**: loads full Spring context with `@ActiveProfiles("test")`, mocks `SimpMessagingTemplate` and `RabbitAutoConfiguration`, uses `TestRabbitTemplate` for direct queue sends
- **`BaseWebSocketTest`**: starts a real server on port 58881 with `@ActiveProfiles("wstest")`, uses WireMock on port 58882 to stub Identity, and `ReceiptChannelInterceptor` (only active in `wstest` profile) to synchronize STOMP frame delivery in tests
- Unit tests mock `IdentityClient` with WireMock stub files under `src/test/resources/test_files/`

### External dependency: Identity service

The Identity service is mocked locally via [Mockoon](https://mockoon.com/) using `mockoon/identity.json`. In tests, WireMock replaces it. The Feign client `IdentityClient` calls two endpoints:
- `POST /api/v1/auth/authenticate` — verifies bearer token, returns `AuthResponse`
- `GET /api/v1/user-data/{tenantId}` — returns `UserDataResponse` with entity access list
