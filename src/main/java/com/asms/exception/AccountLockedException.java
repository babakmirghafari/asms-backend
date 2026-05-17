package com.asms.exception;

import java.time.OffsetDateTime;

/**
 * Thrown when a login attempt is made against a locked account (HTTP 423).
 */
public class AccountLockedException extends AsmsException {

    private final OffsetDateTime lockedUntil;

    public AccountLockedException(OffsetDateTime lockedUntil) {
        super("ACCOUNT_LOCKED", "Account is temporarily locked due to too many failed login attempts");
        this.lockedUntil = lockedUntil;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }
}
