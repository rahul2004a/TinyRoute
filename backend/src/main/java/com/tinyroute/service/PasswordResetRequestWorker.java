package com.tinyroute.service;

import com.tinyroute.client.PasswordResetMailAdapter;
import com.tinyroute.model.PasswordResetRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
public class PasswordResetRequestWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetRequestWorker.class);

    private final PasswordResetMailAdapter passwordResetMailAdapter;
    private final PasswordResetRequestProcessor passwordResetRequestProcessor;
    private final TaskExecutor passwordResetExecutor;
    private final String confirmationUri;

    public PasswordResetRequestWorker(
            PasswordResetMailAdapter passwordResetMailAdapter,
            PasswordResetRequestProcessor passwordResetRequestProcessor,
            @Qualifier("passwordResetExecutor") TaskExecutor passwordResetExecutor,
            @Value("${tinyroute.mail.password-reset-confirmation-uri}") String confirmationUri
    ) {
        this.passwordResetMailAdapter = Objects.requireNonNull(passwordResetMailAdapter);
        this.passwordResetRequestProcessor = Objects.requireNonNull(passwordResetRequestProcessor);
        this.passwordResetExecutor = Objects.requireNonNull(passwordResetExecutor);
        this.confirmationUri = Objects.requireNonNull(confirmationUri);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(PasswordResetRequested request) {
        try {
            passwordResetExecutor.execute(() -> prepareAndDeliver(request));
        } catch (RuntimeException exception) {
            LOGGER.warn("Password reset request dispatch is temporarily unavailable");
        }
    }

    private void prepareAndDeliver(PasswordResetRequested request) {
        if (!passwordResetRequestProcessor.prepareDelivery(request)) {
            return;
        }
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
