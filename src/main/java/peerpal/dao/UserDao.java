package peerpal.dao;

import peerpal.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL queries related to the users table.
 *
 * NOTE: passwords are stored as plain text on purpose so they can be viewed
 * directly in SQLite (school activity only). Real systems must store hashes.
 */
public class UserDao {

    /**
     * Saves a new user. Status is always PENDING.
     * Returns false if the username is already taken.
     */
    public boolean register(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return false;
            }
            throw new RuntimeException("Register failed: " + e.getMessage(), e);
        }
    }

    /**
     * Checks whether a username already exists.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lookup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the user if the username and password match a row in the database.
     */
    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            rs.getString("status")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns every registered account, regardless of role or status.
     */
    public List<User> getAllUsers() {
        return queryUsers("SELECT id, username, password, roles, status FROM users ORDER BY id");
    }

    /**
     * Returns all accounts (any role) whose status is still PENDING.
     */
    public List<User> getPendingUsers() {
        return queryUsers("SELECT id, username, password, roles, status FROM users "
                + "WHERE status = 'PENDING' ORDER BY id");
    }

    /**
     * Changes a user's status from PENDING to APPROVED.
     * Returns true only if a pending account with that id was updated.
     */
    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Approve failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the role and status of one user (used by the admin "Update User" feature).
     * Both values are converted to UPPERCASE before they reach the database, so the
     * CHECK constraint on the roles column and the status checks elsewhere always match.
     * Returns true only if a row with that id was actually updated.
     */
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    /** Runs a parameterless SELECT and maps each row to a User. */
    private List<User> queryUsers(String sql) {
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }
}
