package com.ipam.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Représente une entrée dans le journal d'audit
 */
public class AuditLog {
    private Long id;
    private String action;          // CREATE, UPDATE, DELETE, ASSIGN, RELEASE
    private String entityType;      // SUBNET, IP, RESERVATION
    private Long entityId;
    private String details;
    private String username;
    private LocalDateTime timestamp;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
        this.username = System.getProperty("user.name");
    }

    public AuditLog(String action, String entityType, Long entityId, String details) {
        this();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return timestamp.format(formatter);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s (ID: %d) - %s", 
            getFormattedTimestamp(), action, entityType, entityId, details);
    }
}
