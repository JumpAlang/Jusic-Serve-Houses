package com.scoder.jusic.controller;

import com.scoder.jusic.model.Token;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class HomeControllerTokenCacheTest {

    @AfterEach
    void resetGlobalToken() {
        HomeController.TOKEN = null;
    }

    @Test
    void concurrentCallersShareOneTokenRefresh() throws Exception {
        HomeController.TOKEN = null;
        HomeController controller = spy(new HomeController());
        Token refreshed = token("cached-token", 7_200L);
        doReturn(refreshed).when(controller).requestToken();
        int callers = 20;
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < callers; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return controller.getToken();
                }));
            }
            start.countDown();
            for (Future<String> result : results) {
                assertEquals("cached-token", result.get(2, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        verify(controller, times(1)).requestToken();
    }

    private Token token(String value, long expiresSeconds) {
        Token token = new Token();
        token.setToken(value);
        token.setExpires(expiresSeconds);
        token.setTokenTime(System.currentTimeMillis());
        return token;
    }
}
