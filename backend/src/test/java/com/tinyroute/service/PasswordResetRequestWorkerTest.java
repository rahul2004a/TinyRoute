package com.tinyroute.service;

import com.tinyroute.client.PasswordResetMailAdapter;
import com.tinyroute.model.PasswordResetRequested;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PasswordResetRequestWorkerTest {

    @Test
    void keepsThePublicAcceptedPathStableWhenTheBoundedWorkerIsSaturated() {
        PasswordResetMailAdapter mailAdapter = mock(PasswordResetMailAdapter.class);
        PasswordResetRequestProcessor processor = mock(PasswordResetRequestProcessor.class);
        PasswordResetRequestWorker worker = new PasswordResetRequestWorker(
                mailAdapter,
                processor,
                task -> {
                    throw new TaskRejectedException("saturated");
                },
                "https://app.example/password-reset/confirm"
        );

        assertThatCode(() -> worker.process(new PasswordResetRequested("user@example.com", "opaque-token")))
                .doesNotThrowAnyException();
        verifyNoInteractions(processor, mailAdapter);
    }
}
