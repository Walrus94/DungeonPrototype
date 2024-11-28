package org.dungeon.prototype.config;

import org.dungeon.prototype.security.TelegramAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"dev", "test"})
public class SecurityConfig {

    @Value("${bot.path}")
    private String endPoint;

    @Bean
    public TelegramAuthenticationProvider telegramAuthenticationProvider() {
        return new TelegramAuthenticationProvider();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(endPoint).authenticated()
                )
                .authenticationManager(authenticationManager(telegramAuthenticationProvider()));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(TelegramAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }
}
