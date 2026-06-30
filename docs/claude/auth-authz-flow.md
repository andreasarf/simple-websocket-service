# Authentication & Authorization Flow

## Overview

Auth and authz are split across two separate Spring mechanisms:
- **Authentication** — `AuthChannelInterceptor` (a `ChannelInterceptor`) fires on `CONNECT`
- **Authorization** — `WebSocketAuthManager` (an `AuthorizationManager`) fires on `SUBSCRIBE`

Both are toggled globally by `app.websocket.auth.enabled`. When disabled, `WebSocketSecurityConfig` sets `anyMessage().permitAll()` and `AuthChannelInterceptor` is not registered at all.

---

## Step 1 — CONNECT (AuthChannelInterceptor)

**File:** `interceptor/AuthChannelInterceptor.java`

1. Intercepts every inbound STOMP frame; acts only on `StompCommand.CONNECT`.
2. Reads `Authorization: Bearer <token>` from the STOMP native header (not the HTTP header — this is a WebSocket frame header).
3. Calls `AuthorizationService.authenticateSignature(token)`:
   - Delegates to `IdentityClient.authenticateSignature()` (Feign, `POST /api/v1/auth/authenticate`)
   - Returns `AuthResponse` → mapped to `UserDetailDto` (contains `tenantId`, `itemUuid`, `email`, raw `token`)
4. Wraps `UserDetailDto` in a `UsernamePasswordAuthenticationToken` with role `USER` and sets it as the session principal via `accessor.setUser(auth)`.
5. If auth fails (exception or missing header), the principal is simply not set — the session is allowed to proceed as anonymous. The SUBSCRIBE step will then reject any protected topic.

**Non-obvious:** Authentication failure at CONNECT does NOT disconnect the client. The client is disconnected later when it tries to SUBSCRIBE to a protected topic, because `WebSocketSecurityConfig` requires `.anyMessage().authenticated()` for anything not explicitly permitted.

---

## Step 2 — SUBSCRIBE (WebSocketAuthManager + WebSocketSecurityConfig)

**Files:** `config/WebSocketSecurityConfig.java`, `interceptor/WebSocketAuthManager.java`

Security rules (in order):
```
CONNECT          → permitAll  (auth handled by AuthChannelInterceptor above)
/app/hello       → permitAll  (HelloController test endpoint)
/topic/greetings → permitAll  (HelloController broadcast)
/topic/**        → access(WebSocketAuthManager)
anyMessage       → authenticated
```

`WebSocketAuthManager.check()`:
1. Extracts the `UsernamePasswordAuthenticationToken` set at CONNECT time from the session.
2. Casts the principal to `UserDetailDto`.
3. Calls `AuthorizationService.isAuthorized(userDetail, topic)`:
   - Calls `IdentityClient.getUserData(tenantId, email, itemUuid, clientSecret)` (`GET /api/v1/user-data/{tenantId}`)
   - Parses the subscribed topic with `TopicTemplate.extractParams()` to get `channelUuid`
   - Checks that the `channelUuid` in the topic matches the `channel.id` in `UserDataResponse.entityItem.itemPath`
4. Returns `AuthorizationDecision(true/false)`.

**Non-obvious:** `getUserData` is called on every SUBSCRIBE, not cached. If the same client subscribes to multiple topics, it hits the Identity service each time.

---

## CSRF Bypass

`WebSocketSecurityConfig` declares an empty `csrfChannelInterceptor()` bean. This overrides Spring Security's default `XorCsrfChannelInterceptor` which cannot be disabled via config — the only way to suppress it is to replace the bean. Without this, every STOMP frame would need a CSRF token header, which is impractical for WebSocket clients.

---

## Identity Service Client

**File:** `feign/IdentityClient.java`

| Method | Endpoint | When called |
|--------|----------|-------------|
| `authenticateSignature` | `POST /api/v1/auth/authenticate` | CONNECT |
| `getUserData` | `GET /api/v1/user-data/{tenantId}` | SUBSCRIBE |

Both calls include `X-Internal-Secret` header with `app.identity.clientSecret`. The mock for local dev is `mockoon/identity.json`.

---

## UserDetailDto as Principal

`UserDetailDto` implements `java.security.Principal`. Its `getName()` returns `email`. This is what `SimpMessagingTemplate.convertAndSendToUser()` uses as the user key for user-specific topic delivery (`/user/topic/...`).
