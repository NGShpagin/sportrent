package ru.saschoolsport.auth.support;

import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FeignTestSupport {

    private FeignTestSupport() {
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.POST, "http://user-service/internal/auth/validate",
                Map.of(), null, StandardCharsets.UTF_8, null);
    }

    public static FeignException unauthorized() {
        return new FeignException.Unauthorized("401", request(), null, Map.of());
    }

    public static FeignException forbidden() {
        return new FeignException.Forbidden("403", request(), null, Map.of());
    }

    public static FeignException notFound() {
        return new FeignException.NotFound("404", request(), null, Map.of());
    }

    public static FeignException serverError() {
        return new FeignException.InternalServerError("500", request(), null, Map.of());
    }
}
