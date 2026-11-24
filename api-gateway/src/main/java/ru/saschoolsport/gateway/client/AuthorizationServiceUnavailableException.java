package ru.saschoolsport.gateway.client;

public class AuthorizationServiceUnavailableException extends RuntimeException {

    public AuthorizationServiceUnavailableException(Throwable cause) {
        super("authz-service is unavailable", cause);
    }
}
