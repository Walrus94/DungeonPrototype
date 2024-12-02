package org.dungeon.prototype.config;

import org.dungeon.prototype.security.TelegramAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Value("${bot.path}")
    private String endPoint;

    @Value("${auth-users}")
    private String authorizedUsers;

    @Bean
    public AuthenticationProvider telegramAuthenticationProvider() {
        return new TelegramAuthenticationProvider(authorizedUsers.isEmpty() ? Collections.emptyList() :
                Arrays.stream(authorizedUsers.split(","))
                        .map(String::trim)
                        .map(Long::valueOf)
                        .collect(Collectors.toList()));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (authorizedUsers.isEmpty()) {
            http.authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers(endPoint).authenticated());
        }
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
