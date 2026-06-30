# Test Infrastructure

## Two Base Classes

### BaseTest — unit/integration (no HTTP server)

**File:** `common/BaseTest.java`  
**Profile:** `test`  
**Config:** `application-test.yaml`

- Excludes `RabbitAutoConfiguration` entirely (`@EnableAutoConfiguration(exclude = {...})`) and imports `RabbitTestConfig` instead.
- `SimpMessagingTemplate` is a `@MockitoBean` — tests verify WebSocket sends with `verify(simpMessagingTemplate)`.
- `TestRabbitTemplate` (bean name `testTemplate`) is injected for sending messages directly to listeners without a real broker.
- `app.websocket.auth.enabled: false` — no identity calls needed.
- `app.websocket.broker.localtest.useSimple: true` — `WebSocketConfig` is excluded by `@Profile("!test")` anyway, so broker mode is moot here.

Use for: listener logic, service logic, payload deserialization.

---

### BaseWebSocketTest — end-to-end (real server + real WebSocket)

**File:** `common/BaseWebSocketTest.java`  
**Profile:** `wstest`  
**Config:** `application-wstest.yaml` (server port 58881, identity URL 58882)

- `@SpringBootTest(webEnvironment = DEFINED_PORT)` — full server on port 58881.
- `@EnableWireMock` stubs the identity service on port 58882 (`@ConfigureWireMock(name = "identity-service", port = 58882)`).
- `SimpMessagingTemplate` is a `@MockitoSpyBean` (not a full mock) — real delivery still happens, but you can verify calls.
- Uses `SockJsClient` + `WebSocketStompClient` with a `ThreadPoolTaskScheduler` backed by virtual threads (`AsyncTestConfig`).
- Imports `RabbitTestConfig` (same as BaseTest) — no real RabbitMQ needed.

**WireMock helpers:**
- `arrangeIdentityAuth()` — stubs `POST /api/v1/auth/authenticate` returning `test_files/auth_response.json`
- `arrangeIdentityUserData(authResponse)` — stubs `GET /api/v1/user-data/{tenantId}` returning `test_files/user_data_response.json`
- `arrangeIdentityUserData(authResponse, file)` — overload for alternate user data fixture

Use for: full STOMP connect/subscribe/receive flows, auth and authz rejection scenarios.

---

## ReceiptChannelInterceptor

**File:** `interceptor/ReceiptChannelInterceptor.java`  
**Active only in:** `wstest` profile (registered in `WebSocketConfig.configureClientInboundChannel`)

The in-memory simple broker does not send STOMP `RECEIPT` frames for `SUBSCRIBE` commands. Tests need to know when a subscription is active before sending a RabbitMQ message (otherwise the message arrives before the subscription is registered and is silently dropped).

This interceptor intercepts `SUBSCRIBE` frames after they are handled, then manually constructs and sends a `RECEIPT` frame back to the client on the outbound channel. The test calls `stompSession.setAutoReceipt(true)` and waits on `receiptFuture.get(1, TimeUnit.SECONDS)` as a synchronization barrier.

**Non-obvious:** this interceptor is NOT in production. It's purely a test synchronization mechanism. `clientOutboundChannel` is `@Lazy` to break a circular dependency that would otherwise occur at startup.

---

## RabbitTestConfig

**File:** `config/RabbitTestConfig.java`

Provides a fully mocked `ConnectionFactory` (via Mockito) so `TestRabbitTemplate` can route messages directly to registered `@RabbitListener` methods without a broker. The mock `Channel` always returns `isOpen() = true`. `AcknowledgeMode` is not set here — listeners call `basicAck`/`basicNack` on the mock channel, which is a no-op.

---

## Test Fixtures

Located in `src/test/resources/test_files/`:

| File | Contents |
|------|----------|
| `auth_response.json` | `AuthResponse` with tenantId, itemUuid, email |
| `user_data_response.json` | `UserDataResponse` where `entityItem.itemPath.channel.id` = `CHANNEL_UUID` (the authorized channel) |
| `user_data_2_response.json` | `UserDataResponse` where channel id does NOT match `CHANNEL_UUID` (unauthorized scenario) |
| `ai_reply.txt` | Freeform text used in listener tests |

`FileTestUtils.readFileToString(path)` loads from the classpath root.
