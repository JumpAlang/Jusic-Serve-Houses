package com.scoder.jusic.security;

import com.scoder.jusic.model.Music;
import com.scoder.jusic.service.imp.MusicServiceImpl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderIsolationAspectTest {

    @Test
    @SuppressWarnings("unchecked")
    void routesAnnotatedInvocationToItsExactWorkloadPool() throws Throwable {
        ProviderCallExecutor executor = mock(ProviderCallExecutor.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        ProviderIsolated annotation = mock(ProviderIsolated.class);
        when(annotation.value()).thenReturn(ProviderWorkload.BACKGROUND);
        when(joinPoint.proceed()).thenReturn("done");
        when(executor.background(any(Callable.class))).thenAnswer(invocation ->
                ((Callable<Object>) invocation.getArgument(0)).call());

        Object result = new ProviderIsolationAspect(executor)
                .isolate(joinPoint, annotation);

        assertEquals("done", result);
        verify(executor).background(any(Callable.class));
        verify(executor, never()).pick(any(Callable.class));
        verify(executor, never()).search(any(Callable.class));
        verify(executor, never()).playback(any(Callable.class));
    }

    @Test
    void onlyNetworkBearingMusicMethodsAreMarked() throws Exception {
        Method providerMethod = MusicServiceImpl.class.getMethod(
                "getWYMusicById", String.class, String.class);
        Method redisOnlyMethod = MusicServiceImpl.class.getMethod(
                "toPick", Music.class, String.class);
        Method playbackMethod = MusicServiceImpl.class.getMethod(
                "musicSwitch", String.class);

        assertEquals(ProviderWorkload.PICK,
                providerMethod.getAnnotation(ProviderIsolated.class).value());
        assertEquals(ProviderWorkload.PLAYBACK,
                playbackMethod.getAnnotation(ProviderIsolated.class).value());
        assertNull(redisOnlyMethod.getAnnotation(ProviderIsolated.class));
    }
}
