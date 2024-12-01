package org.dungeon.prototype.config;

import org.dungeon.prototype.security.TelegramAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${bot.path}")
    private String endPoint;

    @Value("${auth-users}")
    private List<Long> authorizedUsers;

    @Bean
    public AuthenticationProvider telegramAuthenticationProvider() {
        return new TelegramAuthenticationProvider(authorizedUsers);
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
        return http.build();
    }
}
