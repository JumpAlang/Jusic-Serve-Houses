package com.scoder.jusic.service;

import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.DefaultPlaylistSong;
import com.scoder.jusic.repository.MusicDefaultRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DefaultPlaylistImporterTest {
    @Test void mapsBothPlatformsAndPreservesLegacyIds() {
        DefaultPlaylistSong wy=DefaultPlaylistSong.fromProvider(JSONObject.parseObject("{\"id\":123,\"name\":\"歌名\",\"ar\":[{\"name\":\"歌手\"}],\"al\":{\"name\":\"专辑\",\"picUrl\":\"https://example.test/a.jpg\"},\"dt\":215000}"),"wy");
        assertEquals("123",wy.member()); assertEquals("专辑",wy.getAlbum()); assertEquals(215000L,wy.getDuration());
        DefaultPlaylistSong qq=DefaultPlaylistSong.fromProvider(JSONObject.parseObject("{\"songmid\":\"mid\",\"songname\":\"QQ歌\",\"singer\":[{\"name\":\"歌手\"}],\"albumname\":\"专辑\",\"albummid\":\"album-mid\",\"interval\":210}"),"qq");
        assertEquals("mid___qq",qq.member()); assertEquals(210000L,qq.getDuration()); assertTrue(qq.getPictureUrl().contains("album-mid"));
        assertNull(DefaultPlaylistSong.legacy("mid___qq").getName()); assertEquals("mid",DefaultPlaylistSong.legacy("mid___qq").getId());
    }
    @Test void importsMetadataFromProviderWithoutResolvingAudioOrLyrics() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/playlist/detail",e->{byte[] body="{\"code\":200,\"playlist\":{\"trackIds\":[{\"id\":1},{\"id\":2}],\"tracks\":[{\"id\":1,\"name\":\"first\",\"dt\":200000}]}}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});
        server.createContext("/song/detail",e->{byte[] body="{\"code\":200,\"songs\":[{\"id\":2,\"name\":\"second\",\"al\":{\"name\":\"album\"}}]}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();});
        server.start();
        try {
            JusicProperties properties=new JusicProperties(); properties.setMusicServeDomain("http://127.0.0.1:"+server.getAddress().getPort());
            MusicDefaultRepository repository=mock(MusicDefaultRepository.class);
            when(repository.addSongs(anyList(),eq("room"))).thenReturn(2L);
            assertEquals(2,new DefaultPlaylistImporter(repository,properties).append("room",new String[]{"playlist"},"wy"));
            ArgumentCaptor<List> songs=ArgumentCaptor.forClass(List.class); verify(repository).addSongs(songs.capture(),eq("room"));
            assertEquals("first",((DefaultPlaylistSong)songs.getValue().get(0)).getName());
            assertEquals("album",((DefaultPlaylistSong)songs.getValue().get(1)).getAlbum());
        } finally {server.stop(0);}
    }
}
