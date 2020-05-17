package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * @author H
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Override
    public Page<List> pictureSearch(String keyword, HulkPage hulkPage) {
        StringBuilder url = new StringBuilder()
                .append("https://www.52doutu.cn")
                .append("/api")
                .append("?types=search&action=searchpic&wd=")
                .append(keyword)
                .append("&limit=")
                .append(hulkPage.getPageSize())
                .append("&offset=")
                .append(hulkPage.getPageIndex() - 1);
        HttpResponse<String> response = null;
        try {
            response = Unirest.get(url.toString())
                    .asString();

            JSONObject jsonObject = JSONObject.parseObject(response.getBody());

            Pattern pattern = compile("(\\d+)");
            Matcher matcher = pattern.matcher(jsonObject.getString("total"));
            while (matcher.find()) {
                hulkPage.setTotalSize(Integer.parseInt(matcher.group()));
            }

            hulkPage.setData(jsonObject.getJSONArray("rows"));
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return hulkPage;
    }

}
