package com.scoder.jusic;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JusicApplicationTests {

    private static final String url = "http://127.0.0.1:8080";
    @Autowired
    private ChatService chatService;

    @Test
    void contextLoads() {

    }

    @Test
    void testDoutu(){
        HulkPage page = new HulkPage();
        page.setCurrentPage(1);
        page.setPageSize(20);
        chatService.pictureSearch("笑死我了",page);
    }

}
