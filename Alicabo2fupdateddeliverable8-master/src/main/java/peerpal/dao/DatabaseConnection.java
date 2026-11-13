package peerpal.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the SQLite connection.
 * Foreign key constraints are enabled on every connection via PRAGMA.
 * The database file (peerpal.db) is created in the project folder.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:peerpal.db";

    private DatabaseConnection() {
        // Utility class - prevent instantiation
    }

    /**
     * Returns a new connection with foreign keys enabled.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // Enable SQLite Foreign Key constraints on every connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
