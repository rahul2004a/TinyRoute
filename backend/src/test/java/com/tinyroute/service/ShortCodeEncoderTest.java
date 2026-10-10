package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShortCodeEncoderTest {
    static byte[] key() {
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) i;
        return key;
    }

    static byte[] salt() {
        return "tinyroute-test-v1".getBytes(StandardCharsets.US_ASCII);
    }

    final ShortCodeEncoder encoder = new ShortCodeEncoder(key(), salt());

    @Test
    void encodesExactlyEightCharactersWithoutPublishingTheCounterSequence() {
        assertThat(encoder.encode(1).value()).matches("[A-Za-z0-9]{8}").isNotEqualTo("00000001");
        assertThat(encoder.encode(1)).isEqualTo(encoder.encode(1)).isNotEqualTo(encoder.encode(2));
    }

    @Test
    void independentDecryptionRecoversTheIntendedDigitOrderAndWidth() {
        assertThat(decrypt(encoder.encode(1).value())).containsExactly(0, 0, 0, 0, 0, 0, 0, 1);
        assertThat(decrypt(encoder.encode(62).value())).containsExactly(0, 0, 0, 0, 0, 0, 1, 0);
        assertThat(decrypt(encoder.encode(218340105584895L).value()))
                .containsExactly(61, 61, 61, 61, 61, 61, 61, 61);
    }

    int[] decrypt(String code) {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        byte[] input = new byte[8], output = new byte[8];
        for (int i = 0; i < 8; i++) input[i] = (byte) alphabet.indexOf(code.charAt(i));
        var cipher = new FPEFF1Engine();
        cipher.init(false, new FPEParameters(new KeyParameter(key()), 62, salt()));
        cipher.processBlock(input, 0, 8, output, 0);
        int[] digits = new int[8];
        for (int i = 0; i < 8; i++) digits[i] = Byte.toUnsignedInt(output[i]);
        return digits;
    }

    @Test
    void distinctConcurrentAllocationsRemainDistinctWithSharedEncoder() throws Exception {
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> results = new ArrayList<>();
            for (long i = 1; i <= 2000; i++) {
                final long value = i;
                results.add(pool.submit(() -> encoder.encode(value).value()));
            }
            Set<String> codes = new HashSet<>();
            for (var result : results)
                assertThat(codes.add(result.get(10, TimeUnit.SECONDS))).isTrue();
            assertThat(codes).hasSize(2000);
        }
    }

    @Test
    void callerArrayMutationDoesNotChangeTheSharedMapping() {
        var key = key();
        var salt = salt();
        var configured = new ShortCodeEncoder(key, salt);
        var before = configured.encode(62);
        Arrays.fill(key, (byte) 0);
        Arrays.fill(salt, (byte) 0);
        assertThat(configured.encode(62)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 218340105584896L, Long.MAX_VALUE})
    void cannotEncodeValuesOutsideTheCounterNamespace(long value) {
        assertThatThrownBy(() -> encoder.encode(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsafeKeyAndSaltWithoutEchoingEither() {
        assertThatThrownBy(() -> new ShortCodeEncoder(new byte[16], salt()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new ShortCodeEncoder(
                                        key(), "bad salt".getBytes(StandardCharsets.US_ASCII)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("bad salt");
    }
}
