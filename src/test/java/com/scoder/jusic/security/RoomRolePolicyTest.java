package com.scoder.jusic.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomRolePolicyTest {

    @Test
    void legacyRolesHaveOneSharedNullSafePolicy() {
        assertTrue(RoomRolePolicy.isRoot("root"));
        assertTrue(RoomRolePolicy.isPrivileged("root"));
        assertTrue(RoomRolePolicy.isPrivileged("admin"));
        assertTrue(RoomRolePolicy.isPrivileged("root,voter"));
        assertTrue(RoomRolePolicy.isPrivileged("admin,voter"));
        assertTrue(RoomRolePolicy.hasCapability("default,picker", "picker"));
        assertTrue(RoomRolePolicy.hasCapability("default, picker, voter", "picker"));
        assertFalse(RoomRolePolicy.isPrivileged("default,notadmin"));
        assertFalse(RoomRolePolicy.hasCapability("default,notpicker", "picker"));
        assertFalse(RoomRolePolicy.isPrivileged(null));
        assertFalse(RoomRolePolicy.hasCapability(null, "voter"));
    }
}
