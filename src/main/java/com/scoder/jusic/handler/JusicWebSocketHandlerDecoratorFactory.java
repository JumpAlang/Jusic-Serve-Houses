package com.scoder.jusic.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * @author H
 */
@Component
public class JusicWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private final JusicWebSocketHandler jusicWebSocketHandler;

    public JusicWebSocketHandlerDecoratorFactory(JusicWebSocketHandler jusicWebSocketHandler) {
        this.jusicWebSocketHandler = jusicWebSocketHandler;
    }

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return this.jusicWebSocketHandler;
    }
}
