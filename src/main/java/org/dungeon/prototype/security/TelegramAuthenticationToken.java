package org.dungeon.prototype.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class TelegramAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;

    public TelegramAuthenticationToken(Long userId) {
        super(null);
        this.userId = userId;
        setAuthenticated(false);
    }

    public TelegramAuthenticationToken(Long userId, boolean authenticated) {
        super(null);
        this.userId = userId;
        setAuthenticated(authenticated);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}
