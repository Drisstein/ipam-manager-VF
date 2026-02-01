package com.ipam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Représente un sous-réseau
 */
public class Subnet {
    private Long id;
    private String networkAddress;  // Ex: "192.168.1.0"
    private String subnetMask;      // Ex: "255.255.255.0"
    private int cidr;               // Ex: 24
    private String description;
    private Integer vlanId;
    private String gateway;
    private String dnsServers;      // Séparés par virgule
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    
    // Champs calculés (non stockés en DB)
    private transient String firstUsableIp;
    private transient String lastUsableIp;
    private transient String broadcastAddress;
    private transient int totalHosts;
    private transient int usedHosts;

    public Subnet() {
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public Subnet(String networkAddress, int cidr, String description) {
        this();
        this.networkAddress = networkAddress;
        this.cidr = cidr;
        this.description = description;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    public void setNetworkAddress(String networkAddress) {
        this.networkAddress = networkAddress;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public int getCidr() {
        return cidr;
    }

    public void setCidr(int cidr) {
        this.cidr = cidr;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDnsServers() {
        return dnsServers;
    }

    public void setDnsServers(String dnsServers) {
        this.dnsServers = dnsServers;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getFirstUsableIp() {
        return firstUsableIp;
    }

    public void setFirstUsableIp(String firstUsableIp) {
        this.firstUsableIp = firstUsableIp;
    }

    public String getLastUsableIp() {
        return lastUsableIp;
    }

    public void setLastUsableIp(String lastUsableIp) {
        this.lastUsableIp = lastUsableIp;
    }

    public String getBroadcastAddress() {
        return broadcastAddress;
    }

    public void setBroadcastAddress(String broadcastAddress) {
        this.broadcastAddress = broadcastAddress;
    }

    public int getTotalHosts() {
        return totalHosts;
    }

    public void setTotalHosts(int totalHosts) {
        this.totalHosts = totalHosts;
    }

    public int getUsedHosts() {
        return usedHosts;
    }

    public void setUsedHosts(int usedHosts) {
        this.usedHosts = usedHosts;
    }

    public String getNetworkCidr() {
        return networkAddress + "/" + cidr;
    }

    public double getUsagePercentage() {
        if (totalHosts == 0) return 0;
        return (usedHosts * 100.0) / totalHosts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subnet subnet = (Subnet) o;
        return Objects.equals(id, subnet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s/%d - %s", networkAddress, cidr, description);
    }
}
