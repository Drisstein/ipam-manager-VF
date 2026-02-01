package com.ipam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Représente une réservation d'adresse IP
 */
public class Reservation {
    private Long id;
    private Long ipAddressId;
    private String reservedBy;
    private String reason;
    private LocalDateTime expirationDate;
    private LocalDateTime createdDate;
    
    // Champs non stockés en DB
    private transient String ipAddress;
    private transient boolean expired;

    public Reservation() {
        this.createdDate = LocalDateTime.now();
    }

    public Reservation(Long ipAddressId, String reservedBy, String reason, LocalDateTime expirationDate) {
        this();
        this.ipAddressId = ipAddressId;
        this.reservedBy = reservedBy;
        this.reason = reason;
        this.expirationDate = expirationDate;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIpAddressId() {
        return ipAddressId;
    }

    public void setIpAddressId(Long ipAddressId) {
        this.ipAddressId = ipAddressId;
    }

    public String getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(String reservedBy) {
        this.reservedBy = reservedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isExpired() {
        if (expirationDate == null) return false;
        return LocalDateTime.now().isAfter(expirationDate);
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Réservation: %s par %s", ipAddress, reservedBy);
    }
}
