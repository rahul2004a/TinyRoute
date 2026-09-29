package com.tinyroute.client;

import java.util.concurrent.CompletableFuture;

public interface RegistrationMailAdapter {

    CompletableFuture<Void> sendOtp(String email, String otp);
}
