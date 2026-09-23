package com.tinyroute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmationRequest(
        @NotBlank @Size(min = 1, max = 512) String token,
        @NotBlank @Size(min = 12, max = 128) String newPassword
) {
}
