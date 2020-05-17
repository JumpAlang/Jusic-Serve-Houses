package com.scoder.jusic.configuration;

import com.scoder.jusic.handler.JusicWebSocketHandlerDecoratorFactory;
import com.scoder.jusic.interceptor.JusicWebSocketHandshakeInterceptor;
import com.scoder.jusic.interceptor.JusicWebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * @author H
 */
@Configuration
@EnableWebSocketMessageBroker
public class JusicWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JusicWebSocketInterceptor jusicWebSocketInterceptor;
    @Autowired
    private JusicWebSocketHandshakeInterceptor jusicWebSocketHandshakeInterceptor;
    @Autowired
    private JusicWebSocketHandlerDecoratorFactory jusicWebSocketHandlerDecoratorFactory;

    /**
     * 有关客户端建立连接的部分
     *
     * @param registry StompEndpointRegistry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                // 配置连接前缀，客户端建立连接时：localhost:port/server
                .addEndpoint("/server")
                // 添加拦截器
                .addInterceptors(jusicWebSocketHandshakeInterceptor)
                // 允许所有域
                .setAllowedOrigins("*")
                // 支持以 SockJs 的方式建立连接，这是一个备选方案，在 WebSocket 不可用的时候启用
                .withSockJS();
    }

    /**
     * 消息代理相关配置
     *
     * @param registry MessageBrokerRegistry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 配置应用前缀，客户端每次访问的时候需要带上此前缀，比如 /app/hello
//        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 消息入口通道相关配置
     *
     * @param registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jusicWebSocketInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // 加入自定义的 handler
        registry.addDecoratorFactory(jusicWebSocketHandlerDecoratorFactory);
    }
}
