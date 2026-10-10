package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tinyroute.cache.ShortCodeCounter;
import com.tinyroute.exception.*;
import com.tinyroute.repository.LinkRepository;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class ShortCodeGeneratorTest {
    final ShortCodeCounter counter = mock(ShortCodeCounter.class);
    final LinkRepository links = mock(LinkRepository.class);
    final ShortCodeGenerator generator =
            new ShortCodeGenerator(
                    counter,
                    links,
                    new ShortCodeEncoder(ShortCodeEncoderTest.key(), ShortCodeEncoderTest.salt()));

    @Test
    void ordinaryAllocationDoesNotReadDatabaseAvailabilityOrRecoveryState() {
        when(counter.nextValueIfInitialized()).thenReturn(OptionalLong.of(62));
        var result = generator.nextCandidate();
        assertThat(result.generationValue()).isEqualTo(62);
        assertThat(result.code().value()).matches("[A-Za-z0-9]{8}");
        verifyNoInteractions(links);
    }

    @Test
    void missingCounterRecoversFromCommittedGenerationValues() {
        when(counter.nextValueIfInitialized()).thenReturn(OptionalLong.empty());
        when(links.findMaxGenerationValue()).thenReturn(62L);
        when(counter.advanceAndIncrement(62)).thenReturn(63L);
        assertThat(generator.nextCandidate().generationValue()).isEqualTo(63);
    }

    @Test
    void confirmedConflictAdvancesFromTheCommittedFloor() {
        when(links.findMaxGenerationValue()).thenReturn(3844L);
        when(counter.advanceAndIncrement(3844)).thenReturn(3845L);
        assertThat(generator.nextCandidateAfterConflict().generationValue()).isEqualTo(3845);
        verify(counter, never()).nextValueIfInitialized();
    }

    @Test
    void unavailableRedisNeverFallsBackToAnotherGenerator() {
        when(counter.nextValueIfInitialized()).thenThrow(new ServiceUnavailableException());
        assertThatThrownBy(generator::nextCandidate)
                .isInstanceOf(ServiceUnavailableException.class);
        verifyNoInteractions(links);
    }

    @Test
    void databaseRecoveryFailureDoesNotRestartFromZero() {
        when(counter.nextValueIfInitialized()).thenReturn(OptionalLong.empty());
        when(links.findMaxGenerationValue())
                .thenThrow(new DataAccessResourceFailureException("private SQL"));
        assertThatThrownBy(generator::nextCandidate)
                .isInstanceOf(ServiceUnavailableException.class);
        verify(counter, never()).advanceAndIncrement(anyLong());
    }

    @Test
    void invalidCommittedFloorFailsClosed() {
        when(counter.nextValueIfInitialized()).thenReturn(OptionalLong.empty());
        when(links.findMaxGenerationValue()).thenReturn(-1L);
        assertThatThrownBy(generator::nextCandidate)
                .isInstanceOf(ServiceUnavailableException.class);
        verify(counter, never()).advanceAndIncrement(anyLong());
    }

    @Test
    void capacityUsesTheExistingAllocationFailureContract() {
        when(counter.nextValueIfInitialized()).thenThrow(new ShortCodeCounterExhaustedException());
        assertThatThrownBy(generator::nextCandidate)
                .isInstanceOf(CodeAllocationFailedException.class);
    }
}
