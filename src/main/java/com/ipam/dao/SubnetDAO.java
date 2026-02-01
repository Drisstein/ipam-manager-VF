package com.ipam.dao;

import com.ipam.model.Subnet;
import com.ipam.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des sous-réseaux
 */
public class SubnetDAO {
    private static final Logger logger = LoggerFactory.getLogger(SubnetDAO.class);

    /**
     * Crée un nouveau sous-réseau
     */
    public Subnet create(Subnet subnet) throws SQLException {
        String sql = """
            INSERT INTO subnets (network_address, subnet_mask, cidr, description, vlan_id, 
                                 gateway, dns_servers, created_date, modified_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

           try (Connection conn = DatabaseManager.getConnection();
               PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, subnet.getNetworkAddress());
            pstmt.setString(2, subnet.getSubnetMask());
            pstmt.setInt(3, subnet.getCidr());
            pstmt.setString(4, subnet.getDescription());
            
            if (subnet.getVlanId() != null) {
                pstmt.setInt(5, subnet.getVlanId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setString(6, subnet.getGateway());
            pstmt.setString(7, subnet.getDnsServers());
            pstmt.setString(8, subnet.getCreatedDate().toString());
            pstmt.setString(9, subnet.getModifiedDate().toString());

            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                boolean idSet = false;
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs != null && rs.next()) {
                        subnet.setId(rs.getLong(1));
                        idSet = true;
                    }
                } catch (SQLException ex) {
                    // Certains drivers SQLite n'implémentent pas getGeneratedKeys
                    logger.warn("Récupération de clé générée non supportée, tentative last_insert_rowid(): {}", ex.getMessage());
                }

                if (!idSet) {
                    try (Statement s = conn.createStatement();
                         ResultSet rs2 = s.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs2.next()) {
                            subnet.setId(rs2.getLong(1));
                        }
                    }
                }

                logger.info("Sous-réseau créé: {}", subnet.getNetworkCidr());
            }
            return subnet;
        }
    }

    /**
     * Récupère tous les sous-réseaux
     */
    public List<Subnet> findAll() throws SQLException {
        List<Subnet> subnets = new ArrayList<>();
        String sql = "SELECT * FROM subnets ORDER BY network_address";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                subnets.add(mapResultSetToSubnet(rs));
            }
        }
        return subnets;
    }

    /**
     * Récupère un sous-réseau par son ID
     */
    public Subnet findById(Long id) throws SQLException {
        String sql = "SELECT * FROM subnets WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSubnet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Met à jour un sous-réseau
     */
    public void update(Subnet subnet) throws SQLException {
        String sql = """
            UPDATE subnets SET network_address = ?, subnet_mask = ?, cidr = ?, 
                               description = ?, vlan_id = ?, gateway = ?, 
                               dns_servers = ?, modified_date = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            subnet.setModifiedDate(LocalDateTime.now());

            pstmt.setString(1, subnet.getNetworkAddress());
            pstmt.setString(2, subnet.getSubnetMask());
            pstmt.setInt(3, subnet.getCidr());
            pstmt.setString(4, subnet.getDescription());
            
            if (subnet.getVlanId() != null) {
                pstmt.setInt(5, subnet.getVlanId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setString(6, subnet.getGateway());
            pstmt.setString(7, subnet.getDnsServers());
            pstmt.setString(8, subnet.getModifiedDate().toString());
            pstmt.setLong(9, subnet.getId());

            pstmt.executeUpdate();
            logger.info("Sous-réseau mis à jour: {}", subnet.getNetworkCidr());
        }
    }

    /**
     * Supprime un sous-réseau
     */
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM subnets WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.info("Sous-réseau supprimé: ID {}", id);
        }
    }

    /**
     * Vérifie si un sous-réseau existe déjà
     */
    public boolean exists(String networkAddress, int cidr) throws SQLException {
        String sql = "SELECT COUNT(*) FROM subnets WHERE network_address = ? AND cidr = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, networkAddress);
            pstmt.setInt(2, cidr);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Recherche des sous-réseaux par critères
     */
    public List<Subnet> search(String searchTerm) throws SQLException {
        List<Subnet> subnets = new ArrayList<>();
        String sql = """
            SELECT * FROM subnets 
            WHERE network_address LIKE ? 
               OR description LIKE ? 
               OR gateway LIKE ?
            ORDER BY network_address
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + searchTerm + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subnets.add(mapResultSetToSubnet(rs));
                }
            }
        }
        return subnets;
    }

    /**
     * Mappe un ResultSet vers un objet Subnet
     */
    private Subnet mapResultSetToSubnet(ResultSet rs) throws SQLException {
        Subnet subnet = new Subnet();
        subnet.setId(rs.getLong("id"));
        subnet.setNetworkAddress(rs.getString("network_address"));
        subnet.setSubnetMask(rs.getString("subnet_mask"));
        subnet.setCidr(rs.getInt("cidr"));
        subnet.setDescription(rs.getString("description"));
        
        int vlanId = rs.getInt("vlan_id");
        if (!rs.wasNull()) {
            subnet.setVlanId(vlanId);
        }
        
        subnet.setGateway(rs.getString("gateway"));
        subnet.setDnsServers(rs.getString("dns_servers"));
        subnet.setCreatedDate(LocalDateTime.parse(rs.getString("created_date")));
        subnet.setModifiedDate(LocalDateTime.parse(rs.getString("modified_date")));

        return subnet;
    }
}
