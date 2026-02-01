package com.ipam.service;

import com.ipam.dao.AuditLogDAO;
import com.ipam.dao.IPAddressDAO;
import com.ipam.dao.SubnetDAO;
import com.ipam.model.AuditLog;
import com.ipam.model.IPAddress;
import com.ipam.model.IPStatus;
import com.ipam.model.Subnet;
import com.ipam.util.IPCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Service pour la gestion des sous-réseaux
 */
public class SubnetService {
    private static final Logger logger = LoggerFactory.getLogger(SubnetService.class);
    
    private final SubnetDAO subnetDAO;
    private final IPAddressDAO ipAddressDAO;
    private final AuditLogDAO auditLogDAO;

    public SubnetService() {
        this.subnetDAO = new SubnetDAO();
        this.ipAddressDAO = new IPAddressDAO();
        this.auditLogDAO = new AuditLogDAO();
    }

    /**
     * Crée un nouveau sous-réseau avec toutes ses IPs
     */
    public Subnet createSubnet(Subnet subnet) throws SQLException {
        // Validation
        if (!IPCalculator.isValidIP(subnet.getNetworkAddress())) {
            throw new IllegalArgumentException("Adresse réseau invalide");
        }
        
        if (subnet.getCidr() < 0 || subnet.getCidr() > 32) {
            throw new IllegalArgumentException("CIDR invalide (doit être entre 0 et 32)");
        }

        // Calculer les informations du sous-réseau
        String networkAddress = IPCalculator.getNetworkAddress(subnet.getNetworkAddress(), subnet.getCidr());
        subnet.setNetworkAddress(networkAddress);
        subnet.setSubnetMask(IPCalculator.cidrToSubnetMask(subnet.getCidr()));

        // Vérifier si le sous-réseau existe déjà
        if (subnetDAO.exists(networkAddress, subnet.getCidr())) {
            throw new IllegalArgumentException("Ce sous-réseau existe déjà");
        }

        // Créer le sous-réseau
        Subnet created = subnetDAO.create(subnet);

        // Générer toutes les adresses IP du sous-réseau
        generateIPAddresses(created);

        // Audit log
        auditLogDAO.create(new AuditLog("CREATE", "SUBNET", created.getId(), 
            String.format("Sous-réseau créé: %s", created.getNetworkCidr())));

        logger.info("Sous-réseau créé avec succès: {}", created.getNetworkCidr());
        return created;
    }

    /**
     * Génère toutes les adresses IP d'un sous-réseau
     */
    private void generateIPAddresses(Subnet subnet) throws SQLException {
        List<String> allIps = IPCalculator.getAllUsableIps(subnet.getNetworkAddress(), subnet.getCidr());
        
        for (String ip : allIps) {
            IPAddress ipAddress = new IPAddress(ip, subnet.getId());
            ipAddress.setStatus(IPStatus.AVAILABLE);
            
            // Marquer la gateway comme réservée si elle est définie
            if (ip.equals(subnet.getGateway())) {
                ipAddress.setStatus(IPStatus.RESERVED);
                ipAddress.setDescription("Gateway");
            }
            
            ipAddressDAO.create(ipAddress);
        }
        
        logger.info("Générées {} adresses IP pour le sous-réseau {}", allIps.size(), subnet.getNetworkCidr());
    }

    /**
     * Assure que les IPs ont été générées pour un sous-réseau donné.
     * Si aucune IP n'existe encore pour ce sous-réseau, les IPs sont générées.
     */
    public void ensureIpsGenerated(Long subnetId) throws SQLException {
        List<IPAddress> existing = ipAddressDAO.findBySubnetId(subnetId);
        if (existing.isEmpty()) {
            Subnet subnet = subnetDAO.findById(subnetId);
            if (subnet != null) {
                generateIPAddresses(subnet);
            }
        }
    }

    /**
     * Récupère tous les sous-réseaux avec statistiques
     */
    public List<Subnet> getAllSubnets() throws SQLException {
        List<Subnet> subnets = subnetDAO.findAll();
        
        for (Subnet subnet : subnets) {
            enrichSubnetWithStats(subnet);
        }
        
        return subnets;
    }

    /**
     * Enrichit un sous-réseau avec ses statistiques
     */
    private void enrichSubnetWithStats(Subnet subnet) throws SQLException {
        // Calculs IP
        subnet.setFirstUsableIp(IPCalculator.getFirstUsableIp(subnet.getNetworkAddress(), subnet.getCidr()));
        subnet.setLastUsableIp(IPCalculator.getLastUsableIp(subnet.getNetworkAddress(), subnet.getCidr()));
        subnet.setBroadcastAddress(IPCalculator.getBroadcastAddress(subnet.getNetworkAddress(), subnet.getCidr()));
        subnet.setTotalHosts(IPCalculator.getTotalHosts(subnet.getCidr()));
        
        // Statistiques d'utilisation
        int usedHosts = ipAddressDAO.countUsedIpsBySubnet(subnet.getId());
        subnet.setUsedHosts(usedHosts);
    }

    /**
     * Récupère un sous-réseau par ID avec statistiques
     */
    public Subnet getSubnetById(Long id) throws SQLException {
        Subnet subnet = subnetDAO.findById(id);
        if (subnet != null) {
            enrichSubnetWithStats(subnet);
        }
        return subnet;
    }

    /**
     * Met à jour un sous-réseau
     */
    public void updateSubnet(Subnet subnet) throws SQLException {
        Subnet existing = subnetDAO.findById(subnet.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Sous-réseau introuvable");
        }

        // Validation de la gateway
        if (subnet.getGateway() != null && !subnet.getGateway().isEmpty()) {
            if (!IPCalculator.isValidIP(subnet.getGateway())) {
                throw new IllegalArgumentException("Gateway invalide");
            }
            
            if (!IPCalculator.isIpInSubnet(subnet.getGateway(), subnet.getNetworkAddress(), subnet.getCidr())) {
                throw new IllegalArgumentException("La gateway doit appartenir au sous-réseau");
            }
        }

        subnetDAO.update(subnet);

        // Audit log
        auditLogDAO.create(new AuditLog("UPDATE", "SUBNET", subnet.getId(), 
            String.format("Sous-réseau modifié: %s", subnet.getNetworkCidr())));

        logger.info("Sous-réseau mis à jour: {}", subnet.getNetworkCidr());
    }

    /**
     * Supprime un sous-réseau et toutes ses IPs
     */
    public void deleteSubnet(Long id) throws SQLException {
        Subnet subnet = subnetDAO.findById(id);
        if (subnet == null) {
            throw new IllegalArgumentException("Sous-réseau introuvable");
        }

        // Supprimer toutes les IPs associées
        ipAddressDAO.deleteBySubnetId(id);

        // Supprimer le sous-réseau
        subnetDAO.delete(id);

        // Audit log
        auditLogDAO.create(new AuditLog("DELETE", "SUBNET", id, 
            String.format("Sous-réseau supprimé: %s", subnet.getNetworkCidr())));

        logger.info("Sous-réseau supprimé: {}", subnet.getNetworkCidr());
    }

    /**
     * Recherche des sous-réseaux
     */
    public List<Subnet> searchSubnets(String searchTerm) throws SQLException {
        List<Subnet> subnets = subnetDAO.search(searchTerm);
        
        for (Subnet subnet : subnets) {
            enrichSubnetWithStats(subnet);
        }
        
        return subnets;
    }

    /**
     * Vérifie si un sous-réseau se chevauche avec d'autres
     */
    public boolean checkOverlap(String networkAddress, int cidr) throws SQLException {
        List<Subnet> allSubnets = subnetDAO.findAll();
        
        for (Subnet existing : allSubnets) {
            if (IPCalculator.subnetsOverlap(networkAddress, cidr, 
                    existing.getNetworkAddress(), existing.getCidr())) {
                logger.warn("Chevauchement détecté avec le sous-réseau {}", existing.getNetworkCidr());
                return true;
            }
        }
        return false;
    }

    /**
     * Obtient des statistiques globales
     */
    public SubnetStatistics getGlobalStatistics() throws SQLException {
        List<Subnet> allSubnets = getAllSubnets();
        
        int totalSubnets = allSubnets.size();
        int totalIps = 0;
        int usedIps = 0;
        
        for (Subnet subnet : allSubnets) {
            totalIps += subnet.getTotalHosts();
            usedIps += subnet.getUsedHosts();
        }
        
        return new SubnetStatistics(totalSubnets, totalIps, usedIps);
    }

    /**
     * Classe interne pour les statistiques
     */
    public static class SubnetStatistics {
        private final int totalSubnets;
        private final int totalIps;
        private final int usedIps;

        public SubnetStatistics(int totalSubnets, int totalIps, int usedIps) {
            this.totalSubnets = totalSubnets;
            this.totalIps = totalIps;
            this.usedIps = usedIps;
        }

        public int getTotalSubnets() { return totalSubnets; }
        public int getTotalIps() { return totalIps; }
        public int getUsedIps() { return usedIps; }
        public int getAvailableIps() { return totalIps - usedIps; }
        public double getUsagePercentage() { 
            return totalIps > 0 ? (usedIps * 100.0 / totalIps) : 0; 
        }
    }
}
