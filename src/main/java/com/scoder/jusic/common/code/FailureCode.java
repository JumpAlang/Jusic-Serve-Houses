package com.scoder.jusic.common.code;

/**
 * @author H
 */

public enum FailureCode implements Code {

    /**
     * default
     */
    FAILURE("40000", "failure");

    FailureCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private String code;
    private String message;

    @Override
    public String code() {
        return this.code;
    }

    @Override
    public String message() {
        return this.message;
    }
}
