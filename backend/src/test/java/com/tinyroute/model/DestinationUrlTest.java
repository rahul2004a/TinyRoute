package com.tinyroute.model;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.exception.LinkValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DestinationUrlTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://example.com/docs?q=java#setup",
                "HTTPS://Example.com:65535/%2f?q=%23#Frag",
                "https://xn--bcher-kva.example/a",
                "https://127.0.0.1:443/a",
                "https://[2001:db8::1]/a",
                "https://example.com./"
            })
    void preservesTheExactValidDestination(String value) {
        assertThat(DestinationUrl.parse(value, "go.tinyroute.test").value()).isEqualTo(value);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "http://example.com",
                "//example.com",
                "https:/example.com",
                "https://",
                " https://example.com",
                "https://example.com/a b",
                "https://example.com/\n",
                "https://example.com/\\x",
                "https://user:pass@example.com",
                "https://example.com/%zz",
                "https://example.com:0",
                "https://example.com:65536",
                "https://example.com:",
                "https://ex%61mple.com",
                "https://example.com/é",
                "https://127.1",
                "https://2130706433",
                "https://0x7f000001",
                "https://0177.0.0.1",
                "https://127.00.0.1",
                "https://999.0.0.1",
                "https://0x7f.0.0.1",
                "https://a..example.com",
                "https://-example.com",
                "https://[fe80::1%25en0]/"
            })
    void rejectsUnsafeOrAmbiguousAuthoritiesWithoutRepair(String value) {
        assertThatThrownBy(() -> DestinationUrl.parse(value, "go.tinyroute.test"))
                .isInstanceOf(LinkValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://go.tinyroute.test/Abc",
                "HTTPS://GO.TINYROUTE.TEST.:999/a",
                "https://go.tinyroute.test?q=x"
            })
    void rejectsItsOwnShortHostRegardlessOfCasePortOrPath(String value) {
        assertThatThrownBy(() -> DestinationUrl.parse(value, "go.tinyroute.test"))
                .isInstanceOf(LinkValidationException.class);
    }

    @Test
    void comparesEquivalentIpLiteralsWithoutDns() {
        assertThatThrownBy(() -> DestinationUrl.parse("https://[0:0:0:0:0:0:0:1]:443/a", "[::1]"))
                .isInstanceOf(LinkValidationException.class);
        assertThatThrownBy(() -> DestinationUrl.parse("https://[::ffff:127.0.0.1]/", "127.0.0.1"))
                .isInstanceOf(LinkValidationException.class);
        assertThat(
                        DestinationUrl.parse(
                                        "https://go.tinyroute.test.example/a", "go.tinyroute.test")
                                .value())
                .isEqualTo("https://go.tinyroute.test.example/a");
    }

    @Test
    void appliesTheLengthBoundary() {
        String prefix = "https://example.com/";
        assertThat(
                        DestinationUrl.parse(prefix + "a".repeat(8192 - prefix.length()), "go.test")
                                .value())
                .hasSize(8192);
        assertThatThrownBy(
                        () ->
                                DestinationUrl.parse(
                                        prefix + "a".repeat(8193 - prefix.length()), "go.test"))
                .isInstanceOf(LinkValidationException.class);
    }
}
