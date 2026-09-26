package com.scoder.jusic.security;

import com.scoder.jusic.configuration.ProviderIsolationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ProviderCallExecutor {

    private final ThreadPoolTaskExecutor pickExecutor;
    private final ThreadPoolTaskExecutor searchExecutor;
    private final ThreadPoolTaskExecutor playbackExecutor;
    private final ThreadPoolTaskExecutor backgroundExecutor;
    private final ProviderIsolationProperties properties;
    private static final ThreadLocal<Integer> PROVIDER_CALL_DEPTH = new ThreadLocal<>();
    private static final ThreadLocal<Long> PROVIDER_DEADLINE_NANOS = new ThreadLocal<>();

    public ProviderCallExecutor(
            @Qualifier("pickProviderExecutor") ThreadPoolTaskExecutor pickExecutor,
            @Qualifier("searchProviderExecutor") ThreadPoolTaskExecutor searchExecutor,
            @Qualifier("playbackProviderExecutor") ThreadPoolTaskExecutor playbackExecutor,
            @Qualifier("backgroundProviderExecutor") ThreadPoolTaskExecutor backgroundExecutor,
            ProviderIsolationProperties properties) {
        this.pickExecutor = pickExecutor;
        this.searchExecutor = searchExecutor;
        this.playbackExecutor = playbackExecutor;
        this.backgroundExecutor = backgroundExecutor;
        this.properties = properties;
    }

    public <T> T pick(Callable<T> call) {
        return execute(call, pickExecutor, properties.getPick().getTimeoutMs());
    }

    public <T> T search(Callable<T> call) {
        return execute(call, searchExecutor, properties.getSearch().getTimeoutMs());
    }

    public <T> T playback(Callable<T> call) {
        return execute(call, playbackExecutor,
                properties.getPlayback().getTimeoutMs());
    }

    public <T> T background(Callable<T> call) {
        return execute(call, backgroundExecutor,
                properties.getBackground().getTimeoutMs());
    }

    private <T> T execute(Callable<T> call,
                          ThreadPoolTaskExecutor executor,
                          long timeoutMs) {
        // Controller entry points already isolate pick/search calls. Service
        // annotations are deliberately defense-in-depth, so a nested call must
        // execute in the current provider worker instead of submitting to the
        // same bounded pool and waiting on itself.
        if (isProviderCall()) {
            if (shouldAbortCurrentCall()) {
                throw new ProviderCallException("provider call timed out");
            }
            return callInline(call, timeoutMs);
        }

        Future<T> future;
        try {
            future = executor.submit(() -> callInProviderContext(call, timeoutMs));
        } catch (RejectedExecutionException ex) {
            throw new ProviderCallException("provider pool is busy", ex);
        }

        try {
            return future.get(Math.max(1L, timeoutMs), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new ProviderCallException("provider call timed out", ex);
        } catch (InterruptedException ex) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ProviderCallException("provider call interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ProviderCallException) {
                throw (ProviderCallException) cause;
            }
            throw new ProviderCallException("provider call failed", cause);
        }
    }

    public static boolean isProviderCall() {
        Integer depth = PROVIDER_CALL_DEPTH.get();
        return depth != null && depth > 0;
    }

    /**
     * Retry loops use this after catching legacy adapter exceptions. Future
     * cancellation alone is not enough because some HTTP libraries clear or
     * ignore interruption while unwinding a failed request.
     */
    public static boolean shouldAbortCurrentCall() {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        Long deadline = PROVIDER_DEADLINE_NANOS.get();
        return deadline != null && System.nanoTime() - deadline >= 0L;
    }

    private <T> T callInProviderContext(Callable<T> call,
                                        long timeoutMs) throws Exception {
        Integer previousDepth = PROVIDER_CALL_DEPTH.get();
        Long previousDeadline = PROVIDER_DEADLINE_NANOS.get();
        long requestedDeadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(Math.max(1L, timeoutMs));
        long effectiveDeadline = previousDeadline == null
                ? requestedDeadline
                : Math.min(previousDeadline, requestedDeadline);
        PROVIDER_CALL_DEPTH.set(previousDepth == null ? 1 : previousDepth + 1);
        PROVIDER_DEADLINE_NANOS.set(effectiveDeadline);
        try {
            return call.call();
        } finally {
            if (previousDepth == null) {
                PROVIDER_CALL_DEPTH.remove();
            } else {
                PROVIDER_CALL_DEPTH.set(previousDepth);
            }
            if (previousDeadline == null) {
                PROVIDER_DEADLINE_NANOS.remove();
            } else {
                PROVIDER_DEADLINE_NANOS.set(previousDeadline);
            }
        }
    }

    private <T> T callInline(Callable<T> call, long timeoutMs) {
        try {
            return callInProviderContext(call, timeoutMs);
        } catch (ProviderCallException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderCallException("provider call failed", ex);
        }
    }
}
