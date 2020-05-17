package com.scoder.jusic.model;

/**
 * @author H
 */

public enum MessageType {
    /**
     *
     */
    NOTICE("NOTICE"),
    ONLINE("ONLINE"),
    SETTING_NAME("SETTING_NAME"),
    AUTH("AUTH"),
    AUTH_ROOT("AUTH_ROOT"),
    AUTH_ADMIN("AUTH_ADMIN"),
    MUSIC("MUSIC"),
    PICK("PICK"),
    CHAT("CHAT"),
    SEARCH("SEARCH"),
    SEARCH_PICTURE("SEARCH_PICTURE"),
    PAUSE("PAUSE"),
    VOLUMN("VOLUMN"),
    GOODMODEL("GOODMODEL");

    MessageType(String type) {
        this.type = type;
    }

    private String type;

    public String type() {
        return this.type;
    }
}
