package com.scoder.jusic.security;

/** Shared interpretation of legacy room role strings. */
public final class RoomRolePolicy {

    private static final String ROOT = "root";
    private static final String ADMIN = "admin";

    private RoomRolePolicy() {
    }

    public static boolean isRoot(String role) {
        return hasRoleToken(role, ROOT);
    }

    public static boolean isPrivileged(String role) {
        return hasRoleToken(role, ROOT) || hasRoleToken(role, ADMIN);
    }

    public static boolean hasCapability(String role, String capability) {
        return hasRoleToken(role, capability);
    }

    private static boolean hasRoleToken(String role, String expected) {
        if (role == null || expected == null || expected.trim().isEmpty()) {
            return false;
        }
        String checkedExpected = expected.trim();
        String[] tokens = role.split(",");
        for (String token : tokens) {
            if (checkedExpected.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }
}
