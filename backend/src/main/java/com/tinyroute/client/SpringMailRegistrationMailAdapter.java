package com.tinyroute.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SpringMailRegistrationMailAdapter implements RegistrationMailAdapter {

    private final JavaMailSender mailSender;
    private final String sender;

    public SpringMailRegistrationMailAdapter(
            JavaMailSender mailSender,
            @Value("${tinyroute.mail.registration-from}") String sender
    ) {
        this.mailSender = Objects.requireNonNull(mailSender);
        this.sender = Objects.requireNonNull(sender);
    }

    @Override
    @Async("registrationMailExecutor")
    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Verify your TinyRoute email");
        message.setText("Your TinyRoute verification code is " + otp + ". It expires in 10 minutes.");
        mailSender.send(message);
    }
}
