package com.tinyroute.model;

import com.tinyroute.exception.LinkValidationException;
import java.util.Locale;
import java.util.Set;

public record ShortCode(String value) {
    private static final Set<String> RESERVED =
            Set.of(
                    "api",
                    "actuator",
                    "error",
                    "health",
                    "login",
                    "logout",
                    "register",
                    "links",
                    "settings",
                    "analytics",
                    "account",
                    "password-reset");

    public ShortCode {
        if (value == null || !value.matches("[A-Za-z0-9_-]{3,64}"))
            throw new LinkValidationException(
                    "alias", "Use 3–64 letters, numbers, hyphens or underscores.");
    }

    public static ShortCode forAlias(String value) {
        ShortCode code = new ShortCode(value);
        if (isReserved(value))
            throw new LinkValidationException(
                    "alias", "Choose another alias; this name is reserved.");
        return code;
    }

    public static boolean isReserved(String value) {
        return value != null && RESERVED.contains(value.toLowerCase(Locale.ROOT));
    }
}
