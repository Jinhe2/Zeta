package com.zeta.web;

public final class AuthHeaderUtils {

    private AuthHeaderUtils() {
    }

    public static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authorization.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
