package com.tinyroute.client;

import com.tinyroute.model.RegistrationOtpRequested;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
public class RegistrationMailDelivery {

    private final RegistrationMailAdapter registrationMailAdapter;

    public RegistrationMailDelivery(RegistrationMailAdapter registrationMailAdapter) {
        this.registrationMailAdapter = Objects.requireNonNull(registrationMailAdapter);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(RegistrationOtpRequested request) {
        registrationMailAdapter.sendOtp(request.email(), request.otp());
    }
}
