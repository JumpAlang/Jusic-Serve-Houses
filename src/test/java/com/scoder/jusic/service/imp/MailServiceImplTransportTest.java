package com.scoder.jusic.service.imp;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailServiceImplTransportTest {

    @Test
    void serverJUsesBoundedStandardHttpTransportAndUtf8FormData() throws Exception {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/send", exchange -> {
            method.set(exchange.getRequestMethod());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(readUtf8(exchange.getRequestBody()));
            byte[] response = "{\"errno\":0}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/send";

            assertTrue(MailServiceImpl.commonSendJ(
                    url, "你好", "一起听歌", 1_000, 1_000));
            assertEquals("POST", method.get());
            assertEquals("application/x-www-form-urlencoded; charset=UTF-8",
                    contentType.get());
            assertTrue(body.get().contains("text=%E4%BD%A0%E5%A5%BD"));
            assertTrue(body.get().contains("desp=%E4%B8%80%E8%B5%B7%E5%90%AC%E6%AD%8C"));
        } finally {
            server.stop(0);
        }
    }

    private static String readUtf8(InputStream input) throws java.io.IOException {
        try (InputStream source = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[256];
            int count;
            while ((count = source.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

