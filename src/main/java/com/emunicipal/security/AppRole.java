package com.emunicipal.security;

public enum AppRole {
    ADMIN,
    WARD_MEMBER,
    CITIZEN;

    public String asAuthority() {
        return "ROLE_" + name();
    }

    public static AppRole fromStaffRole(String rawRole) {
        if (rawRole == null) {
            return null;
        }

        String normalized = rawRole.trim().toUpperCase();
        return switch (normalized) {
            case "ADMIN" -> ADMIN;
            case "WARD", "WARD_MEMBER" -> WARD_MEMBER;
            default -> null;
        };
    }

    public static AppRole fromInput(String rawRole) {
        if (rawRole == null) {
            return null;
        }

        String normalized = rawRole.trim().toUpperCase();
        return switch (normalized) {
            case "ADMIN" -> ADMIN;
            case "WARD_MEMBER", "WARD" -> WARD_MEMBER;
            case "CITIZEN" -> CITIZEN;
            default -> null;
        };
    }
}
