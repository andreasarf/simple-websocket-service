package dev.andreasarf.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class HttpSecurityConfig {

    /**
     * This bean configures security for standard HTTP requests.
     * It disables the login form and allows all HTTP traffic to pass through.
     * This is what removes the user/password prompt.
     */
    @Bean
    public SecurityFilterChain httpSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                // Define authorization rules for HTTP requests
                .authorizeHttpRequests(auth -> auth
                        // Permit all other HTTP requests
                        .anyRequest().permitAll()
                )
                // Disable the default HTTP Basic authentication, which causes the login prompt
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
