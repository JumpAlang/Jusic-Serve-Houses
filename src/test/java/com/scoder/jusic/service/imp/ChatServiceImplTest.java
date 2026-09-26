package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.configuration.PictureSearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatServiceImplTest {

    private PictureSearchProperties properties;
    private MockRestServiceServer server;
    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new PictureSearchProperties();
        properties.setApiUrl("https://picture.test/apihzbqb.php");
        properties.setFallbackUrl("https://fallback.test/qqbqbss");
        service = new ChatServiceImpl(restTemplate, properties);
    }

    @Test
    void mapsApiHzResultToLegacyPictureShape() {
        server.expect(once(), requestTo(startsWith("https://picture.test/apihzbqb.php?")))
                .andExpect(requestTo(containsString("id=88888888")))
                .andExpect(requestTo(containsString("key=88888888")))
                .andExpect(requestTo(containsString("type=2")))
                .andExpect(requestTo(containsString("words=%E9%AB%98%E5%85%B4")))
                .andExpect(requestTo(containsString("page=1")))
                .andExpect(requestTo(containsString("limit=2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":200,\"page\":\"1\",\"maxpage\":\"61\","
                                + "\"count\":\"122\",\"res\":[\"https://cdn.test/a.gif\"]}",
                        MediaType.APPLICATION_JSON));

        HulkPage page = page(1, 2);
        service.pictureSearch("高兴", page);

        JSONArray rows = (JSONArray) page.getData();
        assertEquals(1, rows.size());
        JSONObject row = rows.getJSONObject(0);
        assertEquals("https://cdn.test/a.gif", row.getString("url"));
        assertEquals(row.getString("url"), row.getString("path"));
        assertEquals("高兴", row.getString("title"));
        assertEquals(122, page.getTotalSize());
        assertEquals(61, page.getTotalPage());
        server.verify();
    }

    @Test
    void fallsBackToQqPictureSearchWhenPrimaryIsUnavailable() {
        server.expect(once(), requestTo(startsWith("https://picture.test/apihzbqb.php?")))
                .andRespond(withServerError());
        server.expect(once(), requestTo(startsWith("https://fallback.test/qqbqbss?")))
                .andExpect(requestTo(containsString("msg=%E9%BE%99%E5%9B%BE")))
                .andExpect(requestTo(containsString("page=2")))
                .andExpect(requestTo(containsString("num=3")))
                .andRespond(withSuccess("{\"code\":0,\"hasMorePage\":true,\"currentPage\":2,"
                                + "\"msg\":\"获取成功\",\"data\":[{\"sticker_num\":4,"
                                + "\"sticker_url\":\"https://biaoqing.gtimg.com/a.gif\","
                                + "\"sticker_size\":\"12kb\",\"sticker_format\":\"gif\"}]}",
                        MediaType.APPLICATION_JSON));

        HulkPage page = page(2, 3);
        service.pictureSearch("龙图", page);

        JSONArray rows = (JSONArray) page.getData();
        assertEquals(1, rows.size());
        JSONObject row = rows.getJSONObject(0);
        assertEquals("https://biaoqing.gtimg.com/a.gif", row.getString("url"));
        assertEquals("gif", row.getString("format"));
        assertEquals("12kb", row.getString("size"));
        assertEquals(7, page.getTotalSize());
        server.verify();
    }

    @Test
    void usesConfiguredApiHzAccount() {
        properties.setApiId("custom-id");
        properties.setApiKey("custom-key");
        server.expect(once(), requestTo(containsString("id=custom-id")))
                .andExpect(requestTo(containsString("key=custom-key")))
                .andRespond(withSuccess("{\"code\":200,\"count\":\"0\",\"res\":[]}",
                        MediaType.APPLICATION_JSON));

        service.pictureSearch("开心", page(1, 10));
        server.verify();
    }

    private HulkPage page(int pageIndex, int pageSize) {
        HulkPage page = new HulkPage();
        page.setPageIndex(pageIndex);
        page.setPageSize(pageSize);
        return page;
    }
}
