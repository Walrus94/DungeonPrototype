package org.dungeon.prototype.config;

import org.dungeon.prototype.security.TelegramAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
    public TelegramAuthenticationProvider telegramAuthenticationProvider() {
        return new TelegramAuthenticationProvider(authorizedUsers);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (authorizedUsers.isEmpty()) {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll()
                    );
        } else {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(endPoint).authenticated()
                    )
                    .authenticationManager(authenticationManager(telegramAuthenticationProvider()));
        }
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(TelegramAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }
}
