package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.OutboundMailboxProperties;
import com.scoder.jusic.model.Chat;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Canary tests for the legacy text-frame protocol consumed by the current frontend.
 *
 * <p>These tests deliberately instantiate {@link SessionServiceImpl} directly and
 * only mock the socket, so they never start Spring or contact Redis/network services.</p>
 */
class SessionServiceImplProtocolContractTest {

    @Test
    void noticeFailureKeepsLegacyFrameAndResponseShape() throws Exception {
        Frame frame = send(
                MessageType.NOTICE,
                Response.failure((Object) null, "请求过于频繁，请稍后再试")
        );

        assertFrameEnvelope(frame, "NOTICE");
        assertEquals("40000", frame.body.getString("code"));
        assertEquals("请求过于频繁，请稍后再试", frame.body.getString("message"));
        assertFalse(frame.body.containsKey("data"), "Fastjson historically omits a null data field");
        assertEquals(2, frame.body.size(), "failure response must stay flat and must not gain fields");
    }

    @Test
    void chatSuccessKeepsTypeFirstLineAndNestsPayloadUnderData() throws Exception {
        Chat chat = new Chat();
        chat.setSessionId("session-1");
        chat.setNickName("测试用户");
        chat.setContent("一起听歌🎵");
        chat.setSendTime(1_720_000_000_000L);

        Frame frame = send(MessageType.CHAT, Response.success(chat));

        assertFrameEnvelope(frame, "CHAT");
        assertSuccessResponse(frame.body);
        JSONObject data = frame.body.getJSONObject("data");
        assertNotNull(data);
        assertEquals("chat", data.getString("type"));
        assertEquals("session-1", data.getString("sessionId"));
        assertEquals("测试用户", data.getString("nickName"));
        assertEquals("一起听歌🎵", data.getString("content"));
        assertFalse(frame.body.containsKey("type"), "domain fields belong inside response.data");
    }

    @Test
    void pickSuccessKeepsTypeFirstLineAndListUnderData() throws Exception {
        Music music = new Music();
        music.setId("song-1");
        music.setName("夜空中最亮的星");
        music.setArtist("逃跑计划");

        Frame frame = send(
                MessageType.PICK,
                Response.success(Collections.singletonList(music), "点歌列表")
        );

        assertFrameEnvelope(frame, "PICK");
        assertEquals("20000", frame.body.getString("code"));
        assertEquals("点歌列表", frame.body.getString("message"));
        JSONArray data = frame.body.getJSONArray("data");
        assertNotNull(data);
        assertEquals(1, data.size());
        assertEquals("song-1", data.getJSONObject(0).getString("id"));
        assertEquals("夜空中最亮的星", data.getJSONObject(0).getString("name"));
    }

    @Test
    void roomBroadcastSerializesOnceAndReusesTheImmutableFrame() throws Exception {
        JusicProperties properties = new JusicProperties();
        WebSocketSession first = openSession("broadcast-1");
        WebSocketSession second = openSession("broadcast-2");
        properties.getSessions("room-a").put(first.getId(), first);
        properties.getSessions("room-a").put(second.getId(), second);
        OutboundMailboxProperties mailboxProperties = new OutboundMailboxProperties();
        SessionServiceImpl service = new SessionServiceImpl(
                properties,
                mock(SessionRepository.class),
                mock(SessionBlackRepository.class),
                mock(MusicVoteRepository.class),
                new SessionOutboundMailboxes(Runnable::run, mailboxProperties),
                mock(com.scoder.jusic.configuration.ClientConnectionRegistry.class)
        );
        CountingJsonPayload payload = new CountingJsonPayload();

        service.send(MessageType.NOTICE, payload, "room-a");

        ArgumentCaptor<TextMessage> firstMessage = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> secondMessage = ArgumentCaptor.forClass(TextMessage.class);
        verify(first).sendMessage(firstMessage.capture());
        verify(second).sendMessage(secondMessage.capture());
        assertEquals(1, payload.serializationCount.get());
        assertSame(firstMessage.getValue(), secondMessage.getValue());
        assertFrameEnvelope(Frame.parse(firstMessage.getValue().getPayload()), "NOTICE");
    }

    private static void assertSuccessResponse(JSONObject body) {
        assertEquals("20000", body.getString("code"));
        assertEquals("success", body.getString("message"));
        assertTrue(body.containsKey("data"));
        assertEquals(3, body.size(), "success response must stay {code,message,data}");
    }

    private static void assertFrameEnvelope(Frame frame, String expectedMessageType) {
        assertEquals(expectedMessageType, frame.lines[0]);
        assertEquals("content-type:application/json", frame.lines[1]);
        assertEquals(
                "content-length:" + frame.json.getBytes(StandardCharsets.UTF_8).length,
                frame.lines[2],
                "content-length is the UTF-8 byte count, not the Java character count"
        );
        assertEquals(3, frame.lines.length);
    }

    private static Frame send(MessageType messageType, Object payload) throws Exception {
        WebSocketSession session = openSession("protocol-session");

        OutboundMailboxProperties properties = new OutboundMailboxProperties();
        SessionOutboundMailboxes mailboxes = new SessionOutboundMailboxes(Runnable::run, properties);
        SessionServiceImpl service = new SessionServiceImpl(
                mock(JusicProperties.class),
                mock(SessionRepository.class),
                mock(SessionBlackRepository.class),
                mock(MusicVoteRepository.class),
                mailboxes,
                mock(com.scoder.jusic.configuration.ClientConnectionRegistry.class)
        );
        service.send(session, messageType, payload);

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        return Frame.parse(messageCaptor.getValue().getPayload());
    }

    private static WebSocketSession openSession(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static final class CountingJsonPayload implements JSONAware {
        private final AtomicInteger serializationCount = new AtomicInteger();

        @Override
        public String toJSONString() {
            serializationCount.incrementAndGet();
            return "{\"value\":\"same-for-every-recipient\"}";
        }
    }

    private static final class Frame {
        private final String[] lines;
        private final String json;
        private final JSONObject body;

        private Frame(String[] lines, String json, JSONObject body) {
            this.lines = lines;
            this.json = json;
            this.body = body;
        }

        private static Frame parse(String payload) {
            int bodyStart = payload.indexOf("\n\n");
            assertTrue(bodyStart > 0, "frame must contain an empty line before its JSON body");
            String[] lines = payload.substring(0, bodyStart).split("\n", -1);
            String json = payload.substring(bodyStart + 2);
            return new Frame(lines, json, JSON.parseObject(json));
        }
    }
}
