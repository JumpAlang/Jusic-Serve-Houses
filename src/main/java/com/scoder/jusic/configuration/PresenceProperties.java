package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Expiring Redis presence for the legacy room-session hash.
 *
 * <p>The existing {@code jusic_session_*} hash remains unchanged for frontend
 * and repository compatibility. A small, additive marker key gives every hash
 * field a lease so a crashed JVM cannot leave an online user forever.</p>
 */
@Component
@ConfigurationProperties(prefix = "jusic.presence")
@Data
public class PresenceProperties {

    private boolean enabled = true;
    private String namespace = "jusic:presence:v1";
    private long ttl = 60_000L;
    private long heartbeatDelay = 15_000L;
    private long cleanupDelay = 30_000L;
    private long candidateGrace = 60_000L;
    private long candidateTtl = 180_000L;
    private int refreshBatchSize = 200;

    public long requiredTtl() {
        if (ttl <= 0L) {
            throw new IllegalStateException("jusic.presence.ttl must be positive");
        }
        if (heartbeatDelay <= 0L) {
            throw new IllegalStateException(
                    "jusic.presence.heartbeat-delay must be positive");
        }
        if (heartbeatDelay > Long.MAX_VALUE / 2L
                || ttl <= heartbeatDelay * 2L) {
            throw new IllegalStateException(
                    "jusic.presence.ttl must be greater than two heartbeat intervals");
        }
        return ttl;
    }

    public long requiredCandidateGrace() {
        requiredTtl();
        if (cleanupDelay <= 0L) {
            throw new IllegalStateException(
                    "jusic.presence.cleanup-delay must be positive");
        }
        if (candidateGrace <= 0L
                || heartbeatDelay > Long.MAX_VALUE / 2L
                || candidateGrace < heartbeatDelay * 2L) {
            throw new IllegalStateException(
                    "jusic.presence.candidate-grace must cover at least two heartbeat intervals");
        }
        return candidateGrace;
    }

    public long requiredCandidateTtl() {
        long grace = requiredCandidateGrace();
        long minimum;
        try {
            minimum = Math.addExact(
                    grace, Math.multiplyExact(cleanupDelay, 2L));
        } catch (ArithmeticException ex) {
            throw new IllegalStateException(
                    "jusic.presence candidate timing values are too large", ex);
        }
        if (candidateTtl <= grace || candidateTtl < minimum) {
            throw new IllegalStateException(
                    "jusic.presence.candidate-ttl must exceed candidate-grace "
                            + "and cover two cleanup intervals");
        }
        return candidateTtl;
    }

    public int requiredRefreshBatchSize() {
        if (refreshBatchSize <= 0) {
            throw new IllegalStateException(
                    "jusic.presence.refresh-batch-size must be positive");
        }
        return refreshBatchSize;
    }
}
