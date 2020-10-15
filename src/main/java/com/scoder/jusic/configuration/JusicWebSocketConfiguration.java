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

        //设置简单的消息代理器，它使用Memory（内存）作为消息代理器，
        //其中/user和/topic都是我们发送到前台的数据前缀。前端必须订阅以/user开始的消息（.subscribe()进行监听）。
        //setHeartbeatValue设置后台向前台发送的心跳，
        //注意：setHeartbeatValue这个不能单独设置，不然不起作用，要配合后面setTaskScheduler才可以生效。
        //对应的解决方法的网址：https://stackoverflow.com/questions/39220647/spring-stomp-over-websockets-not-scheduling-heartbeats
//        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
//        te.setPoolSize(1);
//        te.setThreadNamePrefix("wss-heartbeat-thread-");
//        te.initialize();
//        registry.enableSimpleBroker("/topic").setHeartbeatValue(new long[]{25000,25000}).setTaskScheduler(te);;
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
