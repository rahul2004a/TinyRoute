package com.tinyroute.client;

import java.util.concurrent.CompletableFuture;

public interface PasswordResetMailAdapter {

    CompletableFuture<Void> sendPasswordReset(String email, String resetUrl);
}
