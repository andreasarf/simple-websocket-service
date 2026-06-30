package dev.andreasarf.websocket.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import dev.andreasarf.websocket.common.AppHeaders;
import dev.andreasarf.websocket.payload.identity.AuthResponse;
import dev.andreasarf.websocket.payload.identity.UserDataResponse;

import java.util.UUID;

@FeignClient(name = "identity-service", url = "${app.identity.url}")
public interface IdentityClient {

    @PostMapping(value = "/api/v1/auth/authenticate")
    AuthResponse authenticateSignature(@RequestHeader(AppHeaders.AUTHORIZATION) String token,
                                       @RequestHeader(AppHeaders.X_INTERNAL_SECRET) String secret);

    @GetMapping(value = "/api/v1/user-data/{tenantId}")
    UserDataResponse getUserData(@PathVariable("tenantId") Short tenantId,
                                 @RequestParam("email") String email,
                                 @RequestParam("itemUuid") UUID itemUuid,
                                 @RequestHeader(AppHeaders.X_INTERNAL_SECRET) String internalSecret);

}
