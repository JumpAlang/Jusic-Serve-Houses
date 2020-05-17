package com.scoder.jusic.common.code;

import java.io.Serializable;

/**
 * @author H
 */
public interface Code extends Serializable {

    /**
     * code
     *
     * @return String
     */
    String code();

    /**
     * message
     *
     * @return String
     */
    String message();

}
