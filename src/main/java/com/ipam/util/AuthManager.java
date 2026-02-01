package com.ipam.util;

/**
 * Gestion simple de l'authentification (en mémoire)
 */
public class AuthManager {
    private static String currentUser;

    public static void login(String username) {
        currentUser = username;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
