package com.tinyroute.service;

import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.model.GeneratedShortCode;
import com.tinyroute.model.ShortCode;
import java.nio.charset.StandardCharsets;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/** Fixed-key/salt FF1 permutation; no truncation or counter disclosure (FR-CRE-04). */
public final class ShortCodeEncoder {
    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final byte[] key;
    private final byte[] salt;

    public ShortCodeEncoder(byte[] key, byte[] salt) {
        if (key == null
                || key.length != 32
                || salt == null
                || !new String(salt, StandardCharsets.US_ASCII).matches("[A-Za-z0-9:_-]{8,64}"))
            throw new IllegalArgumentException("Short code encoding configuration is invalid");
        this.key = key.clone();
        this.salt = salt.clone();
    }

    public ShortCode encode(long value) {
        if (value < 1 || value > GeneratedShortCode.MAX_VALUE)
            throw new IllegalArgumentException("Short code allocation is out of range");
        byte[] digits = new byte[8];
        long remainder = value;
        for (int i = 7; i >= 0; i--) {
            digits[i] = (byte) (remainder % 62);
            remainder /= 62;
        }
        try {
            var cipher = new FPEFF1Engine();
            cipher.init(true, new FPEParameters(new KeyParameter(key), 62, salt));
            byte[] encrypted = new byte[8];
            cipher.processBlock(digits, 0, digits.length, encrypted, 0);
            var result = new StringBuilder(8);
            for (byte digit : encrypted) result.append(ALPHABET.charAt(Byte.toUnsignedInt(digit)));
            return new ShortCode(result.toString());
        } catch (RuntimeException failure) {
            throw new ServiceUnavailableException(failure);
        }
    }
}
