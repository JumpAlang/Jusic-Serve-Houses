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
    SEARCH_SONGLIST("SEARCH_SONGLIST"),
    SEARCH_PICTURE("SEARCH_PICTURE"),
    PAUSE("PAUSE"),
    VOLUMN("VOLUMN"),
    GOODMODEL("GOODMODEL"),
    ADD_HOUSE("ADD_HOUSE"),
    SEARCH_HOUSE("SEARCH_HOUSE"),
    ENTER_HOUSE("ENTER_HOUSE");

    MessageType(String type) {
        this.type = type;
    }

    private String type;

    public String type() {
        return this.type;
    }
}
