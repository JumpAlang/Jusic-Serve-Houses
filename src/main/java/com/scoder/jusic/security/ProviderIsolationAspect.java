package com.scoder.jusic.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Applies one synchronous, bounded provider budget at service boundaries.
 * ProviderCallExecutor's re-entrant path keeps existing controller-level
 * isolation from submitting recursively to the same pool.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class ProviderIsolationAspect {

    private final ProviderCallExecutor providerCallExecutor;

    public ProviderIsolationAspect(ProviderCallExecutor providerCallExecutor) {
        this.providerCallExecutor = providerCallExecutor;
    }

    @Around("@annotation(isolated)")
    public Object isolate(ProceedingJoinPoint joinPoint,
                          ProviderIsolated isolated) {
        Callable<Object> invocation = () -> proceed(joinPoint);
        switch (isolated.value()) {
            case PICK:
                return providerCallExecutor.pick(invocation);
            case SEARCH:
                return providerCallExecutor.search(invocation);
            case PLAYBACK:
                return providerCallExecutor.playback(invocation);
            case BACKGROUND:
                return providerCallExecutor.background(invocation);
            default:
                throw new IllegalStateException("unsupported provider workload");
        }
    }

    private Object proceed(ProceedingJoinPoint joinPoint) throws Exception {
        try {
            return joinPoint.proceed();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new Exception(ex);
        }
    }
}
