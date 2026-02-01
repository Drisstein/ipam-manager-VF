package com.ipam.service;

import com.ipam.dao.AuditLogDAO;
import com.ipam.dao.IPAddressDAO;
import com.ipam.model.AuditLog;
import com.ipam.model.IPAddress;
import com.ipam.model.IPStatus;
import com.ipam.util.IPCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour la gestion des adresses IP
 */
public class IPAddressService {
    private static final Logger logger = LoggerFactory.getLogger(IPAddressService.class);
    
    private final IPAddressDAO ipAddressDAO;
    private final AuditLogDAO auditLogDAO;

    public IPAddressService() {
        this.ipAddressDAO = new IPAddressDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    /**
     * Récupère toutes les adresses IP
     */
    public List<IPAddress> getAllIPAddresses() throws SQLException {
        return ipAddressDAO.findAll();
    }

    /**
     * Récupère les IPs d'un sous-réseau
     */
    public List<IPAddress> getIPsBySubnet(Long subnetId) throws SQLException {
        return ipAddressDAO.findBySubnetId(subnetId);
    }

    /**
     * Récupère une IP par son adresse
     */
    public IPAddress getIPByAddress(String ipAddress) throws SQLException {
        return ipAddressDAO.findByIpAddress(ipAddress);
    }

    /**
     * Assigne une adresse IP
     */
    public void assignIP(Long ipId, String assignedTo, String macAddress, String description) 
            throws SQLException {
        
        IPAddress ipAddress = ipAddressDAO.findById(ipId);
        if (ipAddress == null) {
            throw new IllegalArgumentException("Adresse IP introuvable");
        }

        if (ipAddress.getStatus() != IPStatus.AVAILABLE) {
            throw new IllegalStateException(
                String.format("L'IP %s n'est pas disponible (statut: %s)", 
                    ipAddress.getIpAddress(), ipAddress.getStatus())
            );
        }

        // Validation de l'adresse MAC
        if (macAddress != null && !macAddress.isEmpty()) {
            if (!IPCalculator.isValidMAC(macAddress)) {
                throw new IllegalArgumentException("Adresse MAC invalide");
            }
            macAddress = IPCalculator.formatMAC(macAddress);
        }

        // Vérifier qu'il n'y a pas de conflit MAC
        if (macAddress != null && !macAddress.isEmpty()) {
            checkMACConflict(macAddress, ipId);
        }

        // Mettre à jour l'IP
        ipAddress.setStatus(IPStatus.ASSIGNED);
        ipAddress.setAssignedTo(assignedTo);
        ipAddress.setMacAddress(macAddress);
        ipAddress.setDescription(description);
        ipAddress.setAssignedDate(LocalDateTime.now());

        ipAddressDAO.update(ipAddress);

        // Audit log
        auditLogDAO.create(new AuditLog("ASSIGN", "IP", ipId, 
            String.format("IP %s assignée à %s", ipAddress.getIpAddress(), assignedTo)));

        logger.info("IP {} assignée à {}", ipAddress.getIpAddress(), assignedTo);
    }

    /**
     * Libère une adresse IP
     */
    public void releaseIP(Long ipId) throws SQLException {
        IPAddress ipAddress = ipAddressDAO.findById(ipId);
        if (ipAddress == null) {
            throw new IllegalArgumentException("Adresse IP introuvable");
        }

        if (ipAddress.getStatus() == IPStatus.AVAILABLE) {
            throw new IllegalStateException("L'IP est déjà disponible");
        }

        String previousAssignedTo = ipAddress.getAssignedTo();

        // Réinitialiser l'IP
        ipAddress.setStatus(IPStatus.AVAILABLE);
        ipAddress.setAssignedTo(null);
        ipAddress.setMacAddress(null);
        ipAddress.setDescription(null);
        ipAddress.setAssignedDate(null);

        ipAddressDAO.update(ipAddress);

        // Audit log
        auditLogDAO.create(new AuditLog("RELEASE", "IP", ipId, 
            String.format("IP %s libérée (était assignée à %s)", 
                ipAddress.getIpAddress(), previousAssignedTo)));

        logger.info("IP {} libérée", ipAddress.getIpAddress());
    }

    /**
     * Réserve une adresse IP
     */
    public void reserveIP(Long ipId, String description) throws SQLException {
        IPAddress ipAddress = ipAddressDAO.findById(ipId);
        if (ipAddress == null) {
            throw new IllegalArgumentException("Adresse IP introuvable");
        }

        if (ipAddress.getStatus() != IPStatus.AVAILABLE) {
            throw new IllegalStateException(
                String.format("L'IP %s n'est pas disponible (statut: %s)", 
                    ipAddress.getIpAddress(), ipAddress.getStatus())
            );
        }

        ipAddress.setStatus(IPStatus.RESERVED);
        ipAddress.setDescription(description);

        ipAddressDAO.update(ipAddress);

        // Audit log
        auditLogDAO.create(new AuditLog("RESERVE", "IP", ipId, 
            String.format("IP %s réservée: %s", ipAddress.getIpAddress(), description)));

        logger.info("IP {} réservée", ipAddress.getIpAddress());
    }

    /**
     * Retire une réservation
     */
    public void unreserveIP(Long ipId) throws SQLException {
        IPAddress ipAddress = ipAddressDAO.findById(ipId);
        if (ipAddress == null) {
            throw new IllegalArgumentException("Adresse IP introuvable");
        }

        if (ipAddress.getStatus() != IPStatus.RESERVED) {
            throw new IllegalStateException("L'IP n'est pas réservée");
        }

        ipAddress.setStatus(IPStatus.AVAILABLE);
        ipAddress.setDescription(null);

        ipAddressDAO.update(ipAddress);

        // Audit log
        auditLogDAO.create(new AuditLog("UNRESERVE", "IP", ipId, 
            String.format("Réservation de l'IP %s retirée", ipAddress.getIpAddress())));

        logger.info("Réservation de l'IP {} retirée", ipAddress.getIpAddress());
    }

    /**
     * Attribue automatiquement la première IP disponible
     */
    public IPAddress assignFirstAvailableIP(Long subnetId, String assignedTo, 
                                           String macAddress, String description) 
            throws SQLException {
        
        List<IPAddress> availableIPs = ipAddressDAO.findAvailableBySubnet(subnetId);
        
        if (availableIPs.isEmpty()) {
            throw new IllegalStateException("Aucune adresse IP disponible dans ce sous-réseau");
        }

        IPAddress firstAvailable = availableIPs.get(0);
        assignIP(firstAvailable.getId(), assignedTo, macAddress, description);
        
        return ipAddressDAO.findById(firstAvailable.getId());
    }

    /**
     * Recherche des adresses IP
     */
    public List<IPAddress> searchIPAddresses(String searchTerm) throws SQLException {
        return ipAddressDAO.search(searchTerm);
    }

    /**
     * Vérifie les conflits d'adresse MAC
     */
    private void checkMACConflict(String macAddress, Long excludeIpId) throws SQLException {
        List<IPAddress> allIPs = ipAddressDAO.findAll();
        
        for (IPAddress ip : allIPs) {
            if (!ip.getId().equals(excludeIpId) && 
                macAddress.equalsIgnoreCase(ip.getMacAddress())) {
                throw new IllegalArgumentException(
                    String.format("Conflit MAC: l'adresse %s est déjà utilisée par %s (%s)", 
                        macAddress, ip.getIpAddress(), ip.getAssignedTo())
                );
            }
        }
    }

    /**
     * Obtient des statistiques par statut
     */
    public IPStatistics getStatistics() throws SQLException {
        List<IPAddress> allIPs = ipAddressDAO.findAll();
        
        int available = 0;
        int assigned = 0;
        int reserved = 0;
        int blocked = 0;
        
        for (IPAddress ip : allIPs) {
            switch (ip.getStatus()) {
                case AVAILABLE -> available++;
                case ASSIGNED -> assigned++;
                case RESERVED -> reserved++;
                case BLOCKED -> blocked++;
            }
        }
        
        return new IPStatistics(available, assigned, reserved, blocked);
    }

    /**
     * Classe interne pour les statistiques
     */
    public static class IPStatistics {
        private final int available;
        private final int assigned;
        private final int reserved;
        private final int blocked;

        public IPStatistics(int available, int assigned, int reserved, int blocked) {
            this.available = available;
            this.assigned = assigned;
            this.reserved = reserved;
            this.blocked = blocked;
        }

        public int getAvailable() { return available; }
        public int getAssigned() { return assigned; }
        public int getReserved() { return reserved; }
        public int getBlocked() { return blocked; }
        public int getTotal() { return available + assigned + reserved + blocked; }
    }

    /**
     * Met à jour une IP
     */
    public void updateIP(IPAddress ipAddress) throws SQLException {
        IPAddress existing = ipAddressDAO.findById(ipAddress.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Adresse IP introuvable");
        }

        // Validation MAC si présente
        if (ipAddress.getMacAddress() != null && !ipAddress.getMacAddress().isEmpty()) {
            if (!IPCalculator.isValidMAC(ipAddress.getMacAddress())) {
                throw new IllegalArgumentException("Adresse MAC invalide");
            }
            ipAddress.setMacAddress(IPCalculator.formatMAC(ipAddress.getMacAddress()));
            checkMACConflict(ipAddress.getMacAddress(), ipAddress.getId());
        }

        ipAddressDAO.update(ipAddress);

        // Audit log
        auditLogDAO.create(new AuditLog("UPDATE", "IP", ipAddress.getId(), 
            String.format("IP %s mise à jour", ipAddress.getIpAddress())));

        logger.info("IP {} mise à jour", ipAddress.getIpAddress());
    }
}
