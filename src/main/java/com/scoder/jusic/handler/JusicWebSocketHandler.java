package com.scoder.jusic.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/**
 * https://blog.csdn.net/qq_32331073/article/details/84444412
 * <p>
 * 配合 Spring 提供的 WebSocketHandlerDecoratorFactory 在不改变代码的情况下
 * 为 SubProtocolWebSocketHandler 增加功能
 *
 * @author H
 */
@Component
@Slf4j
public class JusicWebSocketHandler extends WebSocketHandlerDecorator {

    @Autowired
    private JusicWebSocketHandlerAsync jusicWebSocketHandlerAsync;

    public JusicWebSocketHandler(WebSocketHandler delegate) {
        super(delegate);
    }


    /**
     * WebSocket 握手成功并且连接状态已经打开之后被调用
     * </p>
     * 此阶段可以向所有会话广播当前在线人数，下面要做的就是这个功能
     *
     * @param session session
     * @throws Exception -
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Reserve synchronously before the STOMP delegate can retain this
        // transport. A duplicate is closed with the application-specific
        // status before this callback returns.
        if (!jusicWebSocketHandlerAsync.tryReserveConnection(session)) {
            return;
        }
        try {
            // SubProtocolWebSocketHandler creates its own decorator for STOMP
            // output. Put our shared final write gate underneath it so both
            // that path and the application's direct mailbox path serialize
            // writes to the same physical SockJS session.
            super.afterConnectionEstablished(
                    SerializedWebSocketSession.wrap(session));
            jusicWebSocketHandlerAsync.afterConnectionEstablished(session);
        } catch (TaskRejectedException ex) {
            // The bounded executor is deliberately fail-fast. Do not push the
            // initialization work back onto the WebSocket I/O thread.
            log.warn("websocket initialization rejected, session: {}", session.getId());
            jusicWebSocketHandlerAsync.releaseConnectionReservation(session);
            jusicWebSocketHandlerAsync.markConnectionClosed(session);
            session.close(new CloseStatus(1013, "server overloaded"));
        } catch (Exception ex) {
            // The delegate did not accept the connection, or initialization
            // failed synchronously before it was queued. Release only if this
            // exact session still owns the first-wins reservation.
            jusicWebSocketHandlerAsync.releaseConnectionReservation(session);
            jusicWebSocketHandlerAsync.markConnectionClosed(session);
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception closeFailure) {
                    ex.addSuppressed(closeFailure);
                }
            }
            throw ex;
        }
    }

    /**
     * 在 WebSocket 任一方关闭连接之后或者发生传输错误的时候调用，
     * 此阶段会话可能仍然处于打开状态，
     * 因此不建议此阶段发消息
     *
     * @param session     session
     * @param closeStatus -
     * @throws Exception -
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        // Mark first so a queued establish task cannot insert the session while
        // the delegate is processing this close event.
        jusicWebSocketHandlerAsync.markConnectionClosed(session);
        if (jusicWebSocketHandlerAsync.isDuplicateClientRejected(session)) {
            // The delegate never saw a synchronously rejected duplicate.
            jusicWebSocketHandlerAsync.afterConnectionClosed(
                    session, closeStatus);
            return;
        }
        try {
            super.afterConnectionClosed(session, closeStatus);
        } finally {
            // Cleanup is synchronous on purpose: queue rejection must never
            // leak local/Redis session and vote state.
            jusicWebSocketHandlerAsync.afterConnectionClosed(session, closeStatus);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        jusicWebSocketHandlerAsync.markConnectionClosed(session);
        if (jusicWebSocketHandlerAsync.isDuplicateClientRejected(session)) {
            jusicWebSocketHandlerAsync.afterConnectionClosed(
                    session, CloseStatus.SERVER_ERROR);
            return;
        }
        try {
            super.handleTransportError(session, exception);
        } finally {
            // Do not rely exclusively on a later container close callback.
            // Cleanup is idempotent, so a subsequent afterConnectionClosed is safe.
            jusicWebSocketHandlerAsync.afterConnectionClosed(
                    session, CloseStatus.SERVER_ERROR);
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        }
    }


    /**
     * holder 取自 {@link org.springframework.web.socket.messaging.SubProtocolWebSocketHandler}
     */
    public static class WebSocketSessionHolder {

        private final WebSocketSession session;

        private final long createTime;

        private volatile boolean hasHandledMessages;

        WebSocketSessionHolder(WebSocketSession session) {
            this.session = session;
            this.createTime = System.currentTimeMillis();
        }

        public WebSocketSession getSession() {
            return this.session;
        }

        public long getCreateTime() {
            return this.createTime;
        }

        public void setHasHandledMessages() {
            this.hasHandledMessages = true;
        }

        public boolean hasHandledMessages() {
            return this.hasHandledMessages;
        }

        @Override
        public String toString() {
            return "WebSocketSessionHolder[session=" + this.session + ", createTime=" +
                    this.createTime + ", hasHandledMessages=" + this.hasHandledMessages + "]";
        }
    }

}
