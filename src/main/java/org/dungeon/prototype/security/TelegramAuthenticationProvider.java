package org.dungeon.prototype.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class TelegramAuthenticationProvider implements AuthenticationProvider {

    @Value("${auth-users}")
    private List<Long> authorizedUsers;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Long userId = (Long) authentication.getPrincipal();

        if (authorizedUsers.contains(userId)) {
            return new TelegramAuthenticationToken(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        }

        throw new AuthenticationException("Unauthorized user: " + userId) {};
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TelegramAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
