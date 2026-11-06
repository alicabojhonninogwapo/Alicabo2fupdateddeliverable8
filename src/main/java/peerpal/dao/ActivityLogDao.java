package peerpal.dao;

import peerpal.dao.DatabaseConnection;
import peerpal.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL queries related to the activity_logs table (the audit trail).
 * Every statement is a parameterized PreparedStatement.
 */
public class ActivityLogDao {

    private static final String SELECT_LOGS =
            "SELECT id, user_id, action_type, description, timestamp FROM activity_logs ";

    /**
     * Inserts one audit entry. The timestamp is filled in by the database.
     *
     * Logging must never break the action being logged, so a failure here is
     * reported on System.err instead of being thrown to the caller.
     */
    public void logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("WARNING: could not write audit log (" + actionType + "): " + e.getMessage());
        }
    }

    /** Returns every log entry, newest first. */
    public List<ActivityLog> getAllLogs() {
        return queryLogs(SELECT_LOGS + "ORDER BY timestamp DESC, id DESC", null);
    }

    /** Returns the log entries of one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        return queryLogs(SELECT_LOGS + "WHERE user_id = ? ORDER BY timestamp DESC, id DESC", userId);
    }

    /** Deletes every log entry. Returns how many rows were removed. */
    public int clearAllLogs() {
        String sql = "DELETE FROM activity_logs";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Clearing logs failed: " + e.getMessage(), e);
        }
    }

    /** Runs a SELECT (optionally with one int parameter) and maps each row to an ActivityLog. */
    private List<ActivityLog> queryLogs(String sql, Integer userId) {
        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
            return logs;
        } catch (SQLException e) {
            throw new RuntimeException("Fetching logs failed: " + e.getMessage(), e);
        }
    }
}
