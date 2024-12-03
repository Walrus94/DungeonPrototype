package org.dungeon.prototype.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

@Slf4j
public class TelegramAuthenticationProvider implements AuthenticationProvider {

    private final List<Long> authorizedUsers;

    public TelegramAuthenticationProvider(List<Long> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.debug("Authenticating: {}", authentication);
        long userId = (long) authentication.getPrincipal();

        if (authorizedUsers.isEmpty() || authorizedUsers.contains(userId)) {
            log.debug("Successfully authenticated user:{}", userId);
            return new TelegramAuthenticationToken(userId, true);
        }

        log.debug("Failed to authenticated user:{}", userId);
        return new TelegramAuthenticationToken(userId);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TelegramAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
