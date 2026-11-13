package peerpal.dao;

import peerpal.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL queries related to the activity_logs table (the system audit trail).
 */
public class ActivityLogDao {

    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, action_type, description, timestamp FROM activity_logs ";

    /**
     * Records one action in the audit trail.
     * The timestamp is filled in by the database (local time).
     * A logging failure never crashes the app: the action already happened,
     * so a warning is printed instead.
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
            System.err.println("WARNING: Could not write to the audit log: " + e.getMessage());
        }
    }

    /** Returns every log entry, newest first. */
    public List<ActivityLog> getAllLogs() {
        String sql = SELECT_COLUMNS + "ORDER BY timestamp DESC, id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapRows(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Could not load logs: " + e.getMessage(), e);
        }
    }

    /** Returns the log entries of one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = SELECT_COLUMNS + "WHERE user_id = ? ORDER BY timestamp DESC, id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load logs: " + e.getMessage(), e);
        }
    }

    /** Deletes all log entries. Returns how many rows were removed. */
    public int clearLogs() {
        String sql = "DELETE FROM activity_logs";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not clear logs: " + e.getMessage(), e);
        }
    }

    private List<ActivityLog> mapRows(ResultSet rs) throws SQLException {
        List<ActivityLog> logs = new ArrayList<>();
        while (rs.next()) {
            logs.add(new ActivityLog(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("action_type"),
                    rs.getString("description"),
                    rs.getString("timestamp")));
        }
        return logs;
    }
}
