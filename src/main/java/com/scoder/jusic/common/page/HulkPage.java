package com.scoder.jusic.common.page;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author H
 */
public class HulkPage<T> implements Page<T> {

    @JsonProperty("pageIndex")
    private int pageIndex = 1;
    @JsonProperty("pageSize")
    private int pageSize = 10;
    private int totalSize = 0;
    /**
     * total page
     */
    private int totalPage = 0;
    /**
     * current page = page
     */
    private int currentPage = this.pageIndex;
    private T data;

    @Override
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageIndex() {
        return this.pageIndex;
    }

    @Override
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public int getLimitStart() {
        return (this.pageIndex - 1) * this.pageSize;
    }

    @Override
    public int getLimitEnd() {
        return this.pageSize;
    }

    @Override
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public int getTotalSize() {
        return this.totalSize;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getTotalPage() {
        return this.totalPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    @Override
    public void setData(T data) {
        this.data = data;
    }

    @Override
    public T getData() {
        return this.data;
    }

}
