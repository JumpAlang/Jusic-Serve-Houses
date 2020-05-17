package com.scoder.jusic.common.code;

/**
 * @author H
 */
public enum SuccessCode implements Code {

    /**
     * default ...
     */
    SUCCESS("20000", "success");

    SuccessCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private final String code;
    private final String message;

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String message() {
        return this.message;
    }
}
