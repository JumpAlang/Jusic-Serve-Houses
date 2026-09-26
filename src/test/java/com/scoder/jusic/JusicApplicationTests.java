package com.scoder.jusic;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.job.MusicTopJob;
import com.scoder.jusic.service.ChatService;
import com.scoder.jusic.service.MusicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class JusicApplicationTests {

    private static final String url = "http://127.0.0.1:8080";
    @Autowired
    private ChatService chatService;
    @MockBean
    private MusicTopJob musicTopJob;
    @MockBean
    private MusicService musicService;

    @Test
    void contextLoads() {

    }

    @Test
    @EnabledIfSystemProperty(
            named = "jusic.provider.integration", matches = "true")
    void testDoutu(){
        HulkPage page = new HulkPage();
        page.setCurrentPage(1);
        page.setPageSize(20);
        chatService.pictureSearch("笑死我了",page);
    }

}
