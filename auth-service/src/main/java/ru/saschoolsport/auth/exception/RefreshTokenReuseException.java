package ru.saschoolsport.auth.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * A refresh token that was already rotated was presented again. Either the legitimate client or
 * an attacker holds a stale copy, so the whole session must be closed. Extends
 * {@link IllegalStateException} so it is still handled as a plain 401 by {@link RestExceptionHandler}.
 */
@Getter
public class RefreshTokenReuseException extends IllegalStateException {

    private final transient UUID sessionId;

    public RefreshTokenReuseException(UUID sessionId) {
        super("Refresh token is not active: ROTATED");
        this.sessionId = sessionId;
    }
}
