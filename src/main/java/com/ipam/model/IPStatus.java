package com.ipam.model;

/**
 * Statut d'une adresse IP
 */
public enum IPStatus {
    AVAILABLE("Disponible"),
    ASSIGNED("Assignée"),
    RESERVED("Réservée"),
    BLOCKED("Bloquée");

    private final String displayName;

    IPStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
