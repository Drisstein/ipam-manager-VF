package com.ipam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Représente une adresse IP
 */
public class IPAddress {
    private Long id;
    private String ipAddress;
    private Long subnetId;
    private IPStatus status;
    private String assignedTo;      // Nom de l'équipement/utilisateur
    private String macAddress;
    private String description;
    private LocalDateTime assignedDate;
    private LocalDateTime createdDate;
    
    // Champs non stockés en DB
    private transient String subnetName;

    public IPAddress() {
        this.status = IPStatus.AVAILABLE;
        this.createdDate = LocalDateTime.now();
    }

    public IPAddress(String ipAddress, Long subnetId) {
        this();
        this.ipAddress = ipAddress;
        this.subnetId = subnetId;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(Long subnetId) {
        this.subnetId = subnetId;
    }

    public IPStatus getStatus() {
        return status;
    }

    public void setStatus(IPStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getSubnetName() {
        return subnetName;
    }

    public void setSubnetName(String subnetName) {
        this.subnetName = subnetName;
    }

    public boolean isAvailable() {
        return status == IPStatus.AVAILABLE;
    }

    public boolean isAssigned() {
        return status == IPStatus.ASSIGNED;
    }

    public boolean isReserved() {
        return status == IPStatus.RESERVED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPAddress ipAddress1 = (IPAddress) o;
        return Objects.equals(id, ipAddress1.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", ipAddress, status.getDisplayName());
    }
}
