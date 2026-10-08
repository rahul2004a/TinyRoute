package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {
    @Test
    void producesEightBase62Characters() {
        var generator = new ShortCodeGenerator(new SecureRandom());
        for (int i = 0; i < 100; i++)
            assertThat(generator.nextCandidate()).matches("[A-Za-z0-9]{8}");
    }
}
