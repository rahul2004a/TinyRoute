package com.tinyroute.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeneratedShortCodeTest {
    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 218340105584896L, Long.MAX_VALUE})
    void rejectsUnallocatedOrWrappedCounterValues(long value) {
        assertThatThrownBy(() -> new GeneratedShortCode(new ShortCode("aBC012xy"), value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefgh9", "abcd_efg", "abcd-efg"})
    void rejectsNonGeneratedCodeFormats(String value) {
        assertThatThrownBy(() -> new GeneratedShortCode(new ShortCode(value), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void holdsThePublishedCodeWithoutExposingEncodingSecrets() {
        var result = new GeneratedShortCode(new ShortCode("aBC012xy"), 62);
        assertThat(result.code().value()).isEqualTo("aBC012xy");
        assertThat(result.generationValue()).isEqualTo(62);
    }
}
