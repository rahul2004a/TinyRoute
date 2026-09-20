package com.tinyroute.client;

import com.tinyroute.model.RegistrationOtpRequested;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RegistrationMailDelivery {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationMailDelivery.class);

    private final RegistrationMailAdapter registrationMailAdapter;
    private final RegistrationMailCapacity registrationMailCapacity;

    public RegistrationMailDelivery(
            RegistrationMailAdapter registrationMailAdapter,
            RegistrationMailCapacity registrationMailCapacity
    ) {
        this.registrationMailAdapter = Objects.requireNonNull(registrationMailAdapter);
        this.registrationMailCapacity = Objects.requireNonNull(registrationMailCapacity);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(RegistrationOtpRequested request) {
        try {
            registrationMailAdapter.sendOtp(request.email(), request.otp())
                    .whenComplete((ignored, failure) -> {
                        registrationMailCapacity.release();
                        if (failure != null) {
                            LOGGER.warn("Registration email delivery failed");
                        }
                    });
        } catch (RuntimeException exception) {
            registrationMailCapacity.release();
            LOGGER.warn("Registration email dispatch is temporarily unavailable");
        }
    }
}
