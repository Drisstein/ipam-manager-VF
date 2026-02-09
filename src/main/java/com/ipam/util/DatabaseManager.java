package com.ipam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Gestionnaire de connexion à la base de données SQLite
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_NAME = "ipam.db";
    // Chemin de base de données modifié selon la demande
    private static final String DB_PATH = "C:\\Users\\Drisstein\\Downloads\\ipam-manager-2026\\.ipam\\" + DB_NAME;
    private static volatile boolean initialized = false;

    /**
     * Obtient une connexion à la base de données
     */ 
    public static Connection getConnection() throws SQLException {
        try {
            // Créer le dossier si nécessaire
            File dbFile = new File(DB_PATH);
            File parentDir = dbFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Connexion SQLite (nouvelle connexion par appel)
            String url = "jdbc:sqlite:" + DB_PATH;
            Connection conn = DriverManager.getConnection(url);

            // Initialiser le schéma une seule fois
            if (!initialized) {
                synchronized (DatabaseManager.class) {
                    if (!initialized) {
                        initializeSchema(conn);
                        initialized = true;
                        logger.info("Schéma de base de données initialisé avec succès");
                    }
                }
            }

            return conn;
        } catch (SQLException e) {
            logger.error("Erreur lors de la connexion à la base de données", e);
            throw e;
        }
    }

    /**
     * Initialise le schéma de la base de données
     */
    private static void initializeSchema(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Table Subnets
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subnets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    network_address TEXT NOT NULL,
                    subnet_mask TEXT NOT NULL,
                    cidr INTEGER NOT NULL,
                    description TEXT,
                    vlan_id INTEGER,
                    gateway TEXT,
                    dns_servers TEXT,
                    created_date TEXT NOT NULL,
                    modified_date TEXT NOT NULL,
                    UNIQUE(network_address, cidr)
                )
            """);

            // Table IPAddresses
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ip_addresses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ip_address TEXT NOT NULL,
                    subnet_id INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    assigned_to TEXT,
                    mac_address TEXT,
                    description TEXT,
                    assigned_date TEXT,
                    created_date TEXT NOT NULL,
                    FOREIGN KEY (subnet_id) REFERENCES subnets(id) ON DELETE CASCADE,
                    UNIQUE(ip_address)
                )
            """);

            // Table Reservations
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ip_address_id INTEGER NOT NULL,
                    reserved_by TEXT NOT NULL,
                    reason TEXT,
                    expiration_date TEXT,
                    created_date TEXT NOT NULL,
                    FOREIGN KEY (ip_address_id) REFERENCES ip_addresses(id) ON DELETE CASCADE
                )
            """);

            // Table AuditLogs
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id INTEGER NOT NULL,
                    details TEXT,
                    username TEXT NOT NULL,
                    timestamp TEXT NOT NULL
                )
            """);

            // Index pour améliorer les performances
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_subnet ON ip_addresses(subnet_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_status ON ip_addresses(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp)");

            // Peupler la base avec quelques données d'exemple si elle est vide
            seedInitialData(connection);
        }
    }

    /**
     * Insère quelques sous-réseaux et adresses IP d'exemple si la base est vide.
     */
    private static void seedInitialData(Connection connection) throws SQLException {
        // Vérifier s'il y a déjà des données
        try (Statement checkStmt = connection.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM subnets")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Données déjà présentes : ne rien faire
                return;
            }
        }

        String now = LocalDateTime.now().toString();

        // Préparation des requêtes d'insertion
        String insertSubnetSql = "INSERT INTO subnets (network_address, subnet_mask, cidr, description, vlan_id, " +
                "gateway, dns_servers, created_date, modified_date) VALUES (?,?,?,?,?,?,?,?,?)";
        String insertIpSql = "INSERT INTO ip_addresses (ip_address, subnet_id, status, assigned_to, " +
                "mac_address, description, assigned_date, created_date) VALUES (?,?,?,?,?,?,?,?)";

        connection.setAutoCommit(false);
        try {
            long officeSubnetId;
            long guestSubnetId;
            long serversSubnetId;

            // Sous-réseau principal bureau
            try (PreparedStatement ps = connection.prepareStatement(insertSubnetSql);
                 Statement idStmt = connection.createStatement()) {
                int cidr = 24;
                ps.setString(1, "192.168.1.0");
                ps.setString(2, IPCalculator.cidrToSubnetMask(cidr));
                ps.setInt(3, cidr);
                ps.setString(4, "LAN Bureau principal");
                ps.setInt(5, 10);
                ps.setString(6, "192.168.1.1");
                ps.setString(7, "8.8.8.8,8.8.4.4");
                ps.setString(8, now);
                ps.setString(9, now);
                ps.executeUpdate();

                try (ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    rs.next();
                    officeSubnetId = rs.getLong(1);
                }
            }

            // Sous-réseau invités
            try (PreparedStatement ps = connection.prepareStatement(insertSubnetSql);
                 Statement idStmt = connection.createStatement()) {
                int cidr = 24;
                ps.setString(1, "192.168.2.0");
                ps.setString(2, IPCalculator.cidrToSubnetMask(cidr));
                ps.setInt(3, cidr);
                ps.setString(4, "LAN Invités");
                ps.setInt(5, 20);
                ps.setString(6, "192.168.2.1");
                ps.setString(7, "1.1.1.1,1.0.0.1");
                ps.setString(8, now);
                ps.setString(9, now);
                ps.executeUpdate();

                try (ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    rs.next();
                    guestSubnetId = rs.getLong(1);
                }
            }

            // Sous-réseau serveurs
            try (PreparedStatement ps = connection.prepareStatement(insertSubnetSql);
                 Statement idStmt = connection.createStatement()) {
                int cidr = 24;
                ps.setString(1, "10.0.0.0");
                ps.setString(2, IPCalculator.cidrToSubnetMask(cidr));
                ps.setInt(3, cidr);
                ps.setString(4, "Serveurs internes");
                ps.setInt(5, 30);
                ps.setString(6, "10.0.0.1");
                ps.setString(7, "9.9.9.9,149.112.112.112");
                ps.setString(8, now);
                ps.setString(9, now);
                ps.executeUpdate();

                try (ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    rs.next();
                    serversSubnetId = rs.getLong(1);
                }
            }

            // Quelques IPs d'exemple
            try (PreparedStatement psIp = connection.prepareStatement(insertIpSql)) {
                // Bureau principal
                psIp.setString(1, "192.168.1.10");
                psIp.setLong(2, officeSubnetId);
                psIp.setString(3, "ASSIGNED");
                psIp.setString(4, "Serveur Fichiers");
                psIp.setString(5, "AA:BB:CC:DD:EE:01");
                psIp.setString(6, "Serveur de fichiers principal");
                psIp.setString(7, now);
                psIp.setString(8, now);
                psIp.executeUpdate();

                psIp.setString(1, "192.168.1.20");
                psIp.setLong(2, officeSubnetId);
                psIp.setString(3, "AVAILABLE");
                psIp.setString(4, null);
                psIp.setString(5, null);
                psIp.setString(6, "Poste libre");
                psIp.setNull(7, Types.VARCHAR);
                psIp.setString(8, now);
                psIp.executeUpdate();

                // Invités
                psIp.setString(1, "192.168.2.50");
                psIp.setLong(2, guestSubnetId);
                psIp.setString(3, "ASSIGNED");
                psIp.setString(4, "Client WiFi");
                psIp.setString(5, "AA:BB:CC:DD:EE:02");
                psIp.setString(6, "Client invité");
                psIp.setString(7, now);
                psIp.setString(8, now);
                psIp.executeUpdate();

                // Serveurs
                psIp.setString(1, "10.0.0.10");
                psIp.setLong(2, serversSubnetId);
                psIp.setString(3, "ASSIGNED");
                psIp.setString(4, "Serveur Web");
                psIp.setString(5, "AA:BB:CC:DD:EE:03");
                psIp.setString(6, "Serveur d'applications");
                psIp.setString(7, now);
                psIp.setString(8, now);
                psIp.executeUpdate();
            }

            connection.commit();
            logger.info("Données d'exemple insérées dans la base IPAM");
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Erreur lors de l'initialisation des données de démonstration", e);
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Ferme la connexion à la base de données
     */
    public static void closeConnection() {
        // Les connexions sont gérées via try-with-resources dans les DAO ; rien à fermer ici.
    }

    /**
     * Réinitialise la base de données (ATTENTION: supprime toutes les données)
     */
    public static void resetDatabase() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS audit_logs");
            stmt.execute("DROP TABLE IF EXISTS reservations");
            stmt.execute("DROP TABLE IF EXISTS ip_addresses");
            stmt.execute("DROP TABLE IF EXISTS subnets");
            logger.info("Base de données réinitialisée");
            initializeSchema(conn);
        }
    }

    /**
     * Obtient le chemin de la base de données
     */
    public static String getDatabasePath() {
        return DB_PATH;
    }
}
