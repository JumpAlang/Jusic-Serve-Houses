package com.scoder.jusic.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void getNickNameToleratesMissingPersistedFields() {
        User user = User.builder().build();

        assertEquals("()", user.getNickName());
    }

    @Test
    void getNickNameKeepsLegacyDisplayFormat() {
        User user = User.builder()
                .name("Alice")
                .remoteAddress("192.0.2.10")
                .build();

        assertEquals("Alice(192.0.*.*)", user.getNickName());
    }
}
