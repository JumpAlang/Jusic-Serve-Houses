package com.scoder.jusic.service;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;

import java.util.List;

/**
 * @author H
 */
public interface ChatService {

    /**
     * picture search
     * Uses domestic Chinese picture-provider APIs while retaining the legacy
     * page/row shape.
     *
     * @param content  keyword
     * @param hulkPage page
     * @return page
     */
    Page<List> pictureSearch(String content, HulkPage hulkPage);
}
