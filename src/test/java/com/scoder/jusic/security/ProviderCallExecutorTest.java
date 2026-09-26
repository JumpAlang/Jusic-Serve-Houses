package com.scoder.jusic.security;

import com.scoder.jusic.configuration.ProviderIsolationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderCallExecutorTest {

    @Test
    @SuppressWarnings("unchecked")
    void returnsProviderResultFromTheIsolatedPool() throws Exception {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        Future<String> future = mock(Future.class);
        ProviderIsolationProperties properties = new ProviderIsolationProperties();
        when(pick.submit(any(Callable.class))).thenReturn(future);
        when(future.get(properties.getPick().getTimeoutMs(), TimeUnit.MILLISECONDS))
                .thenReturn("music");
        ProviderCallExecutor executor = new ProviderCallExecutor(
                pick, search, playback, background, properties);

        assertEquals("music", executor.pick(() -> "ignored by mock"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void timeoutCancelsWorkAndReturnsOnlyAnInternalGenericFailure() throws Exception {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        Future<String> future = mock(Future.class);
        ProviderIsolationProperties properties = new ProviderIsolationProperties();
        when(pick.submit(any(Callable.class))).thenReturn(future);
        when(future.get(properties.getPick().getTimeoutMs(), TimeUnit.MILLISECONDS))
                .thenThrow(new TimeoutException("provider-secret-detail"));
        ProviderCallExecutor executor = new ProviderCallExecutor(
                pick, search, playback, background, properties);

        ProviderCallException exception = assertThrows(
                ProviderCallException.class, () -> executor.pick(() -> "music"));

        assertEquals("provider call timed out", exception.getMessage());
        verify(future).cancel(true);
    }

    @Test
    void saturatedPoolFailsFast() {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        when(pick.submit(any(Callable.class)))
                .thenThrow(new TaskRejectedException("full"));
        ProviderCallExecutor executor =
                new ProviderCallExecutor(pick, search, playback, background,
                        new ProviderIsolationProperties());

        ProviderCallException exception = assertThrows(
                ProviderCallException.class, () -> executor.pick(() -> "music"));

        assertEquals("provider pool is busy", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void nestedProviderCallRunsInlineInsteadOfSubmittingToAnotherPool()
            throws Exception {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        when(pick.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<String> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        ProviderCallExecutor executor = new ProviderCallExecutor(
                pick, search, playback, background, new ProviderIsolationProperties());

        assertEquals("nested", executor.pick(
                () -> executor.search(() -> "nested")));

        verify(pick).submit(any(Callable.class));
        verify(search, never()).submit(any(Callable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void backgroundUsesItsOwnPoolAndBudget() throws Exception {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        Future<String> future = mock(Future.class);
        ProviderIsolationProperties properties = new ProviderIsolationProperties();
        when(background.submit(any(Callable.class))).thenReturn(future);
        when(future.get(properties.getBackground().getTimeoutMs(),
                TimeUnit.MILLISECONDS)).thenReturn("refreshed");
        ProviderCallExecutor executor = new ProviderCallExecutor(
                pick, search, playback, background, properties);

        assertEquals("refreshed", executor.background(() -> "ignored"));
        verify(pick, never()).submit(any(Callable.class));
        verify(search, never()).submit(any(Callable.class));
        verify(playback, never()).submit(any(Callable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void playbackCannotBeStarvedByTheMaintenancePool() throws Exception {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        Future<String> future = mock(Future.class);
        ProviderIsolationProperties properties = new ProviderIsolationProperties();
        when(playback.submit(any(Callable.class))).thenReturn(future);
        when(future.get(properties.getPlayback().getTimeoutMs(),
                TimeUnit.MILLISECONDS)).thenReturn("playing");
        ProviderCallExecutor executor = new ProviderCallExecutor(
                pick, search, playback, background, properties);

        assertEquals("playing", executor.playback(() -> "ignored"));
        verify(background, never()).submit(any(Callable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void providerDeadlineContextIsVisibleOnlyInsideTheWorker() throws Exception {
        ThreadPoolTaskExecutor pick = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor search = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor playback = mock(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor background = mock(ThreadPoolTaskExecutor.class);
        when(pick.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> task = invocation.getArgument(0);
            return CompletableFuture.completedFuture(task.call());
        });
        ProviderCallExecutor executor = new ProviderCallExecutor(
                pick, search, playback, background, new ProviderIsolationProperties());

        assertFalse(ProviderCallExecutor.isProviderCall());
        assertTrue(executor.pick(() -> ProviderCallExecutor.isProviderCall()
                && !ProviderCallExecutor.shouldAbortCurrentCall()));
        assertFalse(ProviderCallExecutor.isProviderCall());
    }
}
