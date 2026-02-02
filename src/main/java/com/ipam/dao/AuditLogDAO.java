package com.ipam.dao;

import com.ipam.model.AuditLog;
import com.ipam.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des logs d'audit
 */
public class AuditLogDAO {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogDAO.class);

    /**
     * Crée une nouvelle entrée d'audit
     */
    public AuditLog create(AuditLog auditLog) throws SQLException {
        String sql = """
            INSERT INTO audit_logs (action, entity_type, entity_id, details, username, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auditLog.getAction());
            pstmt.setString(2, auditLog.getEntityType());
            pstmt.setLong(3, auditLog.getEntityId());
            pstmt.setString(4, auditLog.getDetails());
            pstmt.setString(5, auditLog.getUsername());
            pstmt.setString(6, auditLog.getTimestamp().toString());

            int affected = pstmt.executeUpdate();
            
            if (affected > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        auditLog.setId(rs.getLong(1));
                    }
                }
            }
            return auditLog;
        }
    }

    /**
     * Récupère tous les logs d'audit
     */
    public List<AuditLog> findAll() throws SQLException {
        return findAll(1000); // Limite par défaut
    }

    /**
     * Récupère tous les logs avec limite
     */
    public List<AuditLog> findAll(int limit) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Récupère les logs par type d'entité
     */
    public List<AuditLog> findByEntityType(String entityType, int limit) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE entity_type = ? ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entityType);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Récupère les logs par action
     */
    public List<AuditLog> findByAction(String action, int limit) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE action = ? ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, action);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Récupère les logs dans une plage de dates
     */
    public List<AuditLog> findByDateRange(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Recherche dans les logs
     */
    public List<AuditLog> search(String searchTerm, int limit) throws SQLException {
        List<AuditLog> logs = new ArrayList<>();
        String sql = """
            SELECT * FROM audit_logs 
            WHERE action LIKE ? 
               OR entity_type LIKE ? 
               OR details LIKE ? 
               OR username LIKE ?
            ORDER BY timestamp DESC LIMIT ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + searchTerm + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            pstmt.setInt(5, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Supprime les vieux logs (nettoyage)
     */
    public int deleteOlderThan(LocalDateTime date) throws SQLException {
        String sql = "DELETE FROM audit_logs WHERE timestamp < ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());
            int deleted = pstmt.executeUpdate();
            logger.info("Supprimés {} logs antérieurs à {}", deleted, date);
            return deleted;
        }
    }

    /**
     * Compte le nombre total de logs
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM audit_logs";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Mappe un ResultSet vers un objet AuditLog
     */
    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getLong("entity_id"));
        log.setDetails(rs.getString("details"));
        log.setUsername(rs.getString("username"));
        log.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        return log;
    }
}
