package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.configuration.PictureSearchProperties;
import com.scoder.jusic.security.ProviderIsolated;
import com.scoder.jusic.security.ProviderWorkload;
import com.scoder.jusic.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * @author H
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private static final int MAX_PROVIDER_PAGE_SIZE = 20;

    private final RestTemplate restTemplate;
    private final PictureSearchProperties properties;

    public ChatServiceImpl(@Qualifier("providerRestTemplate") RestTemplate restTemplate,
                           PictureSearchProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    @ProviderIsolated(ProviderWorkload.SEARCH)
    public Page<List> pictureSearch(String keyword, HulkPage hulkPage) {
        // APIHZ documents a hard limit of 20 items. Reflect the normalized
        // value in the returned page so clients do not calculate overlapping
        // pages when they submit a larger generic search page size.
        if (hulkPage.getPageSize() > MAX_PROVIDER_PAGE_SIZE) {
            hulkPage.setPageSize(MAX_PROVIDER_PAGE_SIZE);
        }
        try {
            return searchApiHz(keyword, hulkPage);
        } catch (RuntimeException ex) {
            log.warn("APIHZ picture search failed; using QQ picture fallback: {}", ex.getMessage());
        }

        try {
            return searchQqPictures(keyword, hulkPage);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("all picture providers failed", ex);
        }
    }

    private Page<List> searchApiHz(String keyword, HulkPage hulkPage) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getApiUrl())
                .queryParam("id", properties.getApiId())
                .queryParam("key", properties.getApiKey())
                .queryParam("type", 2)
                .queryParam("words", keyword)
                .queryParam("page", hulkPage.getPageIndex())
                .queryParam("limit", hulkPage.getPageSize())
                .build()
                .encode()
                .toUri();

        JSONObject root = getJson(uri, "APIHZ");
        if (root.getIntValue("code") != 200) {
            throw new IllegalStateException("APIHZ rejected the request: " + root.getString("msg"));
        }
        JSONArray results = root.getJSONArray("res");
        if (results == null) {
            throw new IllegalStateException("APIHZ response has no results");
        }

        JSONArray rows = new JSONArray();
        for (int i = 0; i < results.size(); i++) {
            String url = results.getString(i);
            if (url != null && !url.isEmpty()) {
                rows.add(toCompatibleRow(Integer.toHexString(url.hashCode()), keyword, url));
            }
        }

        int totalSize = root.getIntValue("count");
        if (totalSize == 0 && !rows.isEmpty()) {
            totalSize = hulkPage.getLimitStart() + rows.size();
        }
        applyPage(hulkPage, rows, totalSize);
        return hulkPage;
    }

    private Page<List> searchQqPictures(String keyword, HulkPage hulkPage) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getFallbackUrl())
                .queryParam("msg", keyword)
                .queryParam("page", hulkPage.getPageIndex())
                .queryParam("num", hulkPage.getPageSize())
                .build()
                .encode()
                .toUri();
        JSONObject root = getJson(uri, "QQ picture search");
        if (root.getIntValue("code") != 0) {
            throw new IllegalStateException("QQ picture search rejected the request: "
                    + root.getString("msg"));
        }

        JSONArray data = root.getJSONArray("data");
        if (data == null) {
            throw new IllegalStateException("QQ picture search response has no data");
        }

        JSONArray rows = new JSONArray();
        for (int i = 0; i < data.size(); i++) {
            JSONObject source = data.getJSONObject(i);
            String url = source.getString("sticker_url");
            if (url != null && !url.isEmpty()) {
                JSONObject row = toCompatibleRow(Integer.toHexString(url.hashCode()), keyword, url);
                row.put("format", source.getString("sticker_format"));
                row.put("size", source.getString("sticker_size"));
                rows.add(row);
            }
        }

        int totalSize = hulkPage.getLimitStart() + rows.size();
        if (root.getBooleanValue("hasMorePage")) {
            totalSize += hulkPage.getPageSize();
        }
        applyPage(hulkPage, rows, totalSize);
        return hulkPage;
    }

    private JSONObject getJson(URI uri, String provider) {
        try {
            String body = restTemplate.getForObject(uri, String.class);
            if (body == null || body.trim().isEmpty()) {
                throw new IllegalStateException(provider + " returned an empty response");
            }
            return JSONObject.parseObject(body);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(provider + " request failed", ex);
        }
    }

    private JSONObject toCompatibleRow(String id, String title, String url) {
        JSONObject row = new JSONObject();
        row.put("id", id);
        row.put("title", title == null ? "" : title);
        row.put("url", url);
        row.put("path", url);
        return row;
    }

    private void applyPage(HulkPage hulkPage, JSONArray rows, int totalSize) {
        hulkPage.setCurrentPage(hulkPage.getPageIndex());
        hulkPage.setTotalSize(totalSize);
        hulkPage.setTotalPage(totalSize == 0 ? 0
                : (totalSize + hulkPage.getPageSize() - 1) / hulkPage.getPageSize());
        hulkPage.setData(rows);
    }
}
