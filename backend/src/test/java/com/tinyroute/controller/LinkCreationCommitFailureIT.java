package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class LinkCreationCommitFailureIT extends LinkHttpTestSupport {
    @MockitoSpyBean PlatformTransactionManager transactions;

    @Test
    void anActualRollbackAtTheCommitBoundaryCannotReturnCreationSuccess() throws Exception {
        doAnswer(
                        invocation -> {
                            if ("com.tinyroute.service.LinkService.create"
                                    .equals(
                                            TransactionSynchronizationManager
                                                    .getCurrentTransactionName())) {
                                transactions.rollback(
                                        invocation.getArgument(0, TransactionStatus.class));
                                throw new TransactionSystemException("private commit failure");
                            }
                            return invocation.callRealMethod();
                        })
                .when(transactions)
                .commit(any());
        mvc.perform(creation("{\"destinationUrl\":\"https://example.com\"}", access))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(header().doesNotExist("Location"));
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isZero();
    }
}
