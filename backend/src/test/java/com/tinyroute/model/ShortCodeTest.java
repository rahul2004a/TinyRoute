package com.tinyroute.model;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.exception.LinkValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ShortCodeTest {
    @Test
    void preservesCaseAndPermitsBothNamespaceVariants() {
        assertThat(ShortCode.forAlias("Abc_123-x").value()).isEqualTo("Abc_123-x");
        assertThat(ShortCode.forAlias("abc")).isNotEqualTo(ShortCode.forAlias("Abc"));
        assertThat(ShortCode.forAlias("x".repeat(64)).value()).hasSize(64);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "ab",
                " abc",
                "abc ",
                "a/b",
                "a.b",
                "abc%20",
                "aébc",
                "API",
                "Actuator",
                "error",
                "health",
                "login",
                "logout",
                "register",
                "links",
                "settings",
                "analytics",
                "account",
                "PASSWORD-RESET"
            })
    void rejectsInvalidAndReservedAliases(String value) {
        assertThatThrownBy(() -> ShortCode.forAlias(value))
                .isInstanceOf(LinkValidationException.class);
    }

    @Test
    void rejectsOverlongAliases() {
        assertThatThrownBy(() -> ShortCode.forAlias("x".repeat(65)))
                .isInstanceOf(LinkValidationException.class);
    }
}
