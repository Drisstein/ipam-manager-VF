package com.ipam.dao;

import com.ipam.model.IPAddress;
import com.ipam.model.IPStatus;
import com.ipam.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des adresses IP
 */
public class IPAddressDAO {
    private static final Logger logger = LoggerFactory.getLogger(IPAddressDAO.class);

    /**
     * Crée une nouvelle adresse IP
     */
    public IPAddress create(IPAddress ipAddress) throws SQLException {
        String sql = """
            INSERT INTO ip_addresses (ip_address, subnet_id, status, assigned_to, 
                                      mac_address, description, assigned_date, created_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

           try (Connection conn = DatabaseManager.getConnection();
               PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, ipAddress.getIpAddress());
            pstmt.setLong(2, ipAddress.getSubnetId());
            pstmt.setString(3, ipAddress.getStatus().name());
            pstmt.setString(4, ipAddress.getAssignedTo());
            pstmt.setString(5, ipAddress.getMacAddress());
            pstmt.setString(6, ipAddress.getDescription());
            
            if (ipAddress.getAssignedDate() != null) {
                pstmt.setString(7, ipAddress.getAssignedDate().toString());
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }
            
            pstmt.setString(8, ipAddress.getCreatedDate().toString());

            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                boolean idSet = false;
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs != null && rs.next()) {
                        ipAddress.setId(rs.getLong(1));
                        idSet = true;
                    }
                } catch (SQLException ex) {
                    logger.warn("getGeneratedKeys non supporté, on utilise last_insert_rowid(): {}", ex.getMessage());
                }

                if (!idSet) {
                    try (Statement s = conn.createStatement();
                         ResultSet rs2 = s.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs2.next()) {
                            ipAddress.setId(rs2.getLong(1));
                        }
                    }
                }

                logger.debug("Adresse IP créée: {}", ipAddress.getIpAddress());
            }
            return ipAddress;
        }
    }

    /**
     * Récupère toutes les adresses IP
     */
    public List<IPAddress> findAll() throws SQLException {
        List<IPAddress> ipAddresses = new ArrayList<>();
        String sql = """
            SELECT ip.*, s.network_address || '/' || s.cidr as subnet_name
            FROM ip_addresses ip
            LEFT JOIN subnets s ON ip.subnet_id = s.id
            ORDER BY ip.ip_address
        """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ipAddresses.add(mapResultSetToIPAddress(rs));
            }
        }
        return ipAddresses;
    }

    /**
     * Récupère toutes les IPs d'un sous-réseau
     */
    public List<IPAddress> findBySubnetId(Long subnetId) throws SQLException {
        List<IPAddress> ipAddresses = new ArrayList<>();
        String sql = """
            SELECT ip.*, s.network_address || '/' || s.cidr as subnet_name
            FROM ip_addresses ip
            LEFT JOIN subnets s ON ip.subnet_id = s.id
            WHERE ip.subnet_id = ?
            ORDER BY ip.ip_address
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, subnetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ipAddresses.add(mapResultSetToIPAddress(rs));
                }
            }
        }
        return ipAddresses;
    }

    /**
     * Récupère une IP par son adresse
     */
    public IPAddress findByIpAddress(String ipAddress) throws SQLException {
        String sql = """
            SELECT ip.*, s.network_address || '/' || s.cidr as subnet_name
            FROM ip_addresses ip
            LEFT JOIN subnets s ON ip.subnet_id = s.id
            WHERE ip.ip_address = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ipAddress);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToIPAddress(rs);
                }
            }
        }
        return null;
    }

    /**
     * Récupère une IP par son ID
     */
    public IPAddress findById(Long id) throws SQLException {
        String sql = """
            SELECT ip.*, s.network_address || '/' || s.cidr as subnet_name
            FROM ip_addresses ip
            LEFT JOIN subnets s ON ip.subnet_id = s.id
            WHERE ip.id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToIPAddress(rs);
                }
            }
        }
        return null;
    }

    /**
     * Met à jour une adresse IP
     */
    public void update(IPAddress ipAddress) throws SQLException {
        String sql = """
            UPDATE ip_addresses SET status = ?, assigned_to = ?, mac_address = ?, 
                                    description = ?, assigned_date = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ipAddress.getStatus().name());
            pstmt.setString(2, ipAddress.getAssignedTo());
            pstmt.setString(3, ipAddress.getMacAddress());
            pstmt.setString(4, ipAddress.getDescription());
            
            if (ipAddress.getAssignedDate() != null) {
                pstmt.setString(5, ipAddress.getAssignedDate().toString());
            } else {
                pstmt.setNull(5, Types.VARCHAR);
            }
            
            pstmt.setLong(6, ipAddress.getId());

            pstmt.executeUpdate();
            logger.debug("Adresse IP mise à jour: {}", ipAddress.getIpAddress());
        }
    }

    /**
     * Supprime une adresse IP
     */
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM ip_addresses WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.debug("Adresse IP supprimée: ID {}", id);
        }
    }

    /**
     * Supprime toutes les IPs d'un sous-réseau
     */
    public void deleteBySubnetId(Long subnetId) throws SQLException {
        String sql = "DELETE FROM ip_addresses WHERE subnet_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, subnetId);
            int deleted = pstmt.executeUpdate();
            logger.info("Supprimées {} adresses IP du sous-réseau ID {}", deleted, subnetId);
        }
    }

    /**
     * Compte les IPs utilisées d'un sous-réseau
     */
    public int countUsedIpsBySubnet(Long subnetId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM ip_addresses 
            WHERE subnet_id = ? AND status IN ('ASSIGNED', 'RESERVED')
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, subnetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Recherche des IPs par critères
     */
    public List<IPAddress> search(String searchTerm) throws SQLException {
        List<IPAddress> ipAddresses = new ArrayList<>();
        String sql = """
            SELECT ip.*, s.network_address || '/' || s.cidr as subnet_name
            FROM ip_addresses ip
            LEFT JOIN subnets s ON ip.subnet_id = s.id
            WHERE ip.ip_address LIKE ? 
               OR ip.assigned_to LIKE ? 
               OR ip.mac_address LIKE ?
               OR ip.description LIKE ?
            ORDER BY ip.ip_address
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + searchTerm + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ipAddresses.add(mapResultSetToIPAddress(rs));
                }
            }
        }
        return ipAddresses;
    }

    /**
     * Récupère les IPs disponibles d'un sous-réseau
     */
    public List<IPAddress> findAvailableBySubnet(Long subnetId) throws SQLException {
        List<IPAddress> ipAddresses = new ArrayList<>();
        String sql = """
            SELECT ip.*, s.network_address || '/' || s.cidr as subnet_name
            FROM ip_addresses ip
            LEFT JOIN subnets s ON ip.subnet_id = s.id
            WHERE ip.subnet_id = ? AND ip.status = 'AVAILABLE'
            ORDER BY ip.ip_address
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, subnetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ipAddresses.add(mapResultSetToIPAddress(rs));
                }
            }
        }
        return ipAddresses;
    }

    /**
     * Mappe un ResultSet vers un objet IPAddress
     */
    private IPAddress mapResultSetToIPAddress(ResultSet rs) throws SQLException {
        IPAddress ipAddress = new IPAddress();
        ipAddress.setId(rs.getLong("id"));
        ipAddress.setIpAddress(rs.getString("ip_address"));
        ipAddress.setSubnetId(rs.getLong("subnet_id"));
        ipAddress.setStatus(IPStatus.valueOf(rs.getString("status")));
        ipAddress.setAssignedTo(rs.getString("assigned_to"));
        ipAddress.setMacAddress(rs.getString("mac_address"));
        ipAddress.setDescription(rs.getString("description"));
        
        String assignedDate = rs.getString("assigned_date");
        if (assignedDate != null) {
            ipAddress.setAssignedDate(LocalDateTime.parse(assignedDate));
        }
        
        ipAddress.setCreatedDate(LocalDateTime.parse(rs.getString("created_date")));
        
        String subnetName = rs.getString("subnet_name");
        if (subnetName != null) {
            ipAddress.setSubnetName(subnetName);
        }

        return ipAddress;
    }
}
