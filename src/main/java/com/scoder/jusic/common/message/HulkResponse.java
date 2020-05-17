package com.scoder.jusic.common.message;


import com.scoder.jusic.common.code.Code;
import com.scoder.jusic.common.code.FailureCode;
import com.scoder.jusic.common.code.SuccessCode;

import java.io.Serializable;

/**
 * @author H
 */
public class HulkResponse<T> implements Response<T>, Serializable {

    private static final long serialVersionUID = -5508341719984417455L;

    private String code;
    private String message;
    private T data;

    public HulkResponse() {
    }

    private HulkResponse(Code code, T data) {
        this.code = code.code();
        this.data = data;
    }

    private HulkResponse(Code code, String message) {
        this.code = code.code();
        this.message = message;
    }

    private HulkResponse(Code code, String message, T data) {
        this.code = code.code();
        this.message = message;
        this.data = data;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public T getData() {
        return this.data;
    }

    static HulkResponse<Void> success() {
        return new HulkResponse<>(SuccessCode.SUCCESS, SuccessCode.SUCCESS.message());
    }

    static <T> HulkResponse<T> success(T data) {
        return new HulkResponse<>(SuccessCode.SUCCESS, SuccessCode.SUCCESS.message(), data);
    }

    static <T> HulkResponse<T> success(T data, String message) {
        return new HulkResponse<>(SuccessCode.SUCCESS, message, data);
    }

    static <T> HulkResponse<T> success(Code code, String message) {
        return new HulkResponse<>(code, message);
    }

    static <T> HulkResponse<T> success(Code code, T data) {
        return new HulkResponse<>(code, data);
    }

    static <T> HulkResponse<T> success(Code code, String message, T data) {
        return new HulkResponse<>(code, message, data);
    }

    static <T> HulkResponse<T> failure() {
        return new HulkResponse<>(FailureCode.FAILURE, FailureCode.FAILURE.message());
    }

    static <T> HulkResponse<T> failure(T data) {
        return new HulkResponse<>(FailureCode.FAILURE, FailureCode.FAILURE.message(), data);
    }

    static <T> HulkResponse<T> failure(T data, String message) {
        return new HulkResponse<>(FailureCode.FAILURE, message, data);
    }

    static <T> HulkResponse<T> failure(Code code, String message) {
        return new HulkResponse<>(code, message);
    }

    static <T> HulkResponse<T> failure(Code code, T data) {
        return new HulkResponse<>(code, data);
    }

    static <T> HulkResponse<T> failure(Code code, String message, T data) {
        return new HulkResponse<>(code, message, data);
    }

}
