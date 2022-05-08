package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.service.ChatService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author H
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Override
    public Page<List> pictureSearch(String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
//                .append("https://www.52doutu.cn")
//                .append("/api")
//                .append("?types=search&action=searchpic&wd=")
//                .append(keyword)
//                .append("&limit=")
//                .append(hulkPage.getPageSize())
//                .append("&offset=")
//                .append(hulkPage.getPageIndex() - 1);
                .append("https://doutu.lccyy.com/doutu")
                .append("/items")
                .append("?keyword=")
                .append(keyword)
                .append("&pageSize=")
                .append(hulkPage.getPageSize())
                .append("&pageNum=")
                .append(hulkPage.getPageIndex());
        HttpResponse<String> response = null;
        try {
            response = Unirest.post(url.toString()).header("User-Agent","PostmanRuntime/7.26.8").header("Cookie","JSESSIONID=6E7B4196A0E717CC308241DC786BE027")
                    .asString();

            JSONObject jsonObject = JSONObject.parseObject(response.getBody());

//            Pattern pattern = compile("(\\d+)");
//            Matcher matcher = pattern.matcher(jsonObject.getString("total"));
//            while (matcher.find()) {
//                hulkPage.setTotalSize(Integer.parseInt(matcher.group()));
//            }
//
//            hulkPage.setData(jsonObject.getJSONArray("rows"));
            hulkPage.setTotalSize(jsonObject.getInteger("totalSize"));
            hulkPage.setData(jsonObject.getJSONArray("items"));
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return hulkPage;
    }

}
