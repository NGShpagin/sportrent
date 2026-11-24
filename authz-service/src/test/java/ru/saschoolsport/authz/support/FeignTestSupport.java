package ru.saschoolsport.authz.support;

import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FeignTestSupport {

    private FeignTestSupport() {
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "http://user-service/internal/users",
                Map.of(), null, StandardCharsets.UTF_8, null);
    }

    public static FeignException.NotFound notFound() {
        return new FeignException.NotFound("404", request(), null, Map.of());
    }
}
