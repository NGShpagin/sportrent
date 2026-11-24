package ru.saschoolsport.gateway.client;

import reactor.core.publisher.Mono;

import java.util.Set;

public interface UserPermissionsClient {

    Mono<Set<String>> getPermissions(String userId);
}
