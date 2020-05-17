package com.scoder.jusic.common.page;

/**
 * @author H
 */
public interface Page<T> {

    /**
     * set page index
     *
     * @param pageIndex page index
     */
    void setPageIndex(int pageIndex);

    /**
     * get page index
     *
     * @return page index
     */
    int getPageIndex();

    /**
     * set page size
     *
     * @param pageSize page size
     */
    void setPageSize(int pageSize);

    /**
     * get page size
     *
     * @return page size
     */
    int getPageSize();

    /**
     * get limit start
     *
     * @return limit start
     */
    int getLimitStart();

    /**
     * get limit end
     *
     * @return limit end
     */
    int getLimitEnd();

    /**
     * set total size
     *
     * @param totalSize total size
     */
    void setTotalSize(int totalSize);

    /**
     * get total size
     *
     * @return total size
     */
    int getTotalSize();

    /**
     * set data
     *
     * @param data data
     */
    void setData(T data);

    /**
     * get data
     *
     * @return data
     */
    T getData();
}
