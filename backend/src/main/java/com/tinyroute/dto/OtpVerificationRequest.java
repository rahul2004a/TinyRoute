package com.tinyroute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerificationRequest(@NotBlank @Pattern(regexp = "\\d{6}") String otp) {
}
