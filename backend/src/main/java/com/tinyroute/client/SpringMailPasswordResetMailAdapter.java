package com.tinyroute.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class SpringMailPasswordResetMailAdapter implements PasswordResetMailAdapter {

    private final JavaMailSender mailSender;
    private final String sender;

    public SpringMailPasswordResetMailAdapter(
            JavaMailSender mailSender,
            @Value("${tinyroute.mail.registration-from}") String sender
    ) {
        this.mailSender = Objects.requireNonNull(mailSender);
        this.sender = Objects.requireNonNull(sender);
    }

    @Override
    @Async("registrationMailExecutor")
    public CompletableFuture<Void> sendPasswordReset(String email, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Reset your TinyRoute password");
        message.setText("Reset your TinyRoute password: " + resetUrl);
        mailSender.send(message);
        return CompletableFuture.completedFuture(null);
    }
}
