package com.tinyroute.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.text.Normalizer;
import java.util.Locale;

public record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String email) {

    public PasswordResetRequest {
        email = email == null ? null : Normalizer.normalize(email.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }
}
