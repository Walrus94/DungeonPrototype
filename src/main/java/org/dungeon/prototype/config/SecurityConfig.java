package org.dungeon.prototype.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dungeon.prototype.security.TelegramAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
                            .requestMatchers(endPoint).authenticated())
                    .addFilterBefore(chatIdFilter(), Filter.class);
        }
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public OncePerRequestFilter chatIdFilter() {
        return new OncePerRequestFilter() {
            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                try {
                    // Attempt to parse the request body into Update object
                    Update update = objectMapper.readValue(request.getInputStream(), Update.class);
                    String chatId = update.getMessage().getChat().getId().toString();

                    // Check if the chatId is authorized
                    if (!isAuthorizedChatId(chatId)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                        return;
                    }
                } catch (Exception e) {
                    // If request is not a valid Telegram Update, block it as bad request
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request format");
                    return;
                }
                filterChain.doFilter(request, response);
            }

            private boolean isAuthorizedChatId(String chatId) {
                if (authorizedUsers.isEmpty()) {
                    return true; // Allow all requests if no chat IDs are configured
                }
                List<String> chatIdList = Arrays.asList(authorizedUsers.split(","));
                return chatIdList.contains(chatId);
            }
        };
    }
}
