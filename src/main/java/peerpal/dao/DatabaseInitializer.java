package peerpal.dao;

import peerpal.model.Role;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Ensures all required database tables exist before the application starts.
 * Called once from Main before any UI or DAO operations.
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates the users table (and any future tables) if they do not exist yet.
     */
    public static void initialize() {
        // Allowed roles come from the Role enum so the table always matches the code
        String allowedRoles = Arrays.stream(Role.values())
                .map(r -> "'" + r.name() + "'")
                .collect(Collectors.joining(", "));

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles    TEXT NOT NULL CHECK (roles IN (%s)),
                    status   TEXT NOT NULL DEFAULT 'PENDING'
                )
                """.formatted(allowedRoles);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            // Future tables (sessions, subjects, etc.) can be added here
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize the database: " + e.getMessage(), e);
        }
    }
}
