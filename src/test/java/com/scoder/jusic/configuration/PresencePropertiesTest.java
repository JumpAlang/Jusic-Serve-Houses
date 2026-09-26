package com.scoder.jusic.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresencePropertiesTest {

    @Test
    void defaultCandidateTimingProvidesARealHeartbeatGraceWindow() {
        PresenceProperties properties = new PresenceProperties();

        assertEquals(60_000L, properties.requiredCandidateGrace());
        assertEquals(180_000L, properties.requiredCandidateTtl());
        assertEquals(200, properties.requiredRefreshBatchSize());
    }

    @Test
    void candidateGraceMustCoverTwoHeartbeatIntervals() {
        PresenceProperties properties = new PresenceProperties();
        properties.setCandidateGrace(29_999L);

        assertThrows(IllegalStateException.class,
                properties::requiredCandidateGrace);
    }

    @Test
    void candidateTtlMustCoverGraceAndTwoCleanupIntervals() {
        PresenceProperties properties = new PresenceProperties();
        properties.setCandidateTtl(119_999L);

        assertThrows(IllegalStateException.class,
                properties::requiredCandidateTtl);
    }

    @Test
    void refreshBatchSizeMustBeAPositiveInteger() {
        PresenceProperties properties = new PresenceProperties();
        properties.setRefreshBatchSize(1);
        assertEquals(1, properties.requiredRefreshBatchSize());

        properties.setRefreshBatchSize(0);
        assertThrows(IllegalStateException.class,
                properties::requiredRefreshBatchSize);

        properties.setRefreshBatchSize(-1);
        assertThrows(IllegalStateException.class,
                properties::requiredRefreshBatchSize);
    }
}
