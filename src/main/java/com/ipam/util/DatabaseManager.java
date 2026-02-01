package com.ipam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;

/**
 * Gestionnaire de connexion à la base de données SQLite
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_NAME = "ipam.db";
    private static final String DB_PATH = System.getProperty("user.home") + File.separator + ".ipam" + File.separator + DB_NAME;
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
            // Index pour améliorer les performances
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_subnet ON ip_addresses(subnet_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip_status ON ip_addresses(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp)");
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
