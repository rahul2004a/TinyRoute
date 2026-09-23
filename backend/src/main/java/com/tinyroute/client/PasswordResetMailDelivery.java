package com.tinyroute.client;

import com.tinyroute.model.PasswordResetRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
public class PasswordResetMailDelivery {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetMailDelivery.class);

    private final PasswordResetMailAdapter passwordResetMailAdapter;
    private final String confirmationUri;

    public PasswordResetMailDelivery(
            PasswordResetMailAdapter passwordResetMailAdapter,
            @Value("${tinyroute.mail.password-reset-confirmation-uri}") String confirmationUri
    ) {
        this.passwordResetMailAdapter = Objects.requireNonNull(passwordResetMailAdapter);
        this.confirmationUri = Objects.requireNonNull(confirmationUri);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(PasswordResetRequested request) {
        try {
            passwordResetMailAdapter.sendPasswordReset(request.email(), confirmationUri + "#token=" + request.token())
                    .whenComplete((ignored, failure) -> {
                        if (failure != null) {
                            LOGGER.warn("Password reset email delivery failed");
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.warn("Password reset email dispatch is temporarily unavailable");
        }
    }
}
