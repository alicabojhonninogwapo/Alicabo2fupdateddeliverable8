package peerpal.dao;

import peerpal.dao.DatabaseConnection;
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
     * Creates the users, activity_logs and tutoring_sessions tables if they do not exist yet.
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

        // Audit trail: one row per critical action. Deleting a user removes their logs.
        String createActivityLogsTable = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp   DATETIME DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """;

        // Core business process of PeerPal: a student books a session with a tutor
        String createSessionsTable = """
                CREATE TABLE IF NOT EXISTS tutoring_sessions (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    tutor_id   INTEGER NOT NULL,
                    subject    TEXT NOT NULL,
                    status     TEXT NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (tutor_id)   REFERENCES users(id) ON DELETE CASCADE
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createActivityLogsTable);
            stmt.execute(createSessionsTable);
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize the database: " + e.getMessage(), e);
        }
    }
}
