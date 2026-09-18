package com.tinyroute.model;

import java.util.Objects;

public record RegistrationOtpRequested(String email, String otp) {

    public RegistrationOtpRequested {
        Objects.requireNonNull(email);
        Objects.requireNonNull(otp);
    }
}
