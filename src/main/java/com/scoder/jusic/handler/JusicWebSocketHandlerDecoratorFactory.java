package com.scoder.jusic.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * @author H
 */
@Component
public class JusicWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    @Autowired
    @Lazy
    private JusicWebSocketHandler jusicWebSocketHandler;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return this.jusicWebSocketHandler;
    }
}
