package peerpal.model;

/**
 * Immutable data model for a row in the activity_logs table.
 */
public record ActivityLog(int id, int userId, String actionType, String description, String timestamp) {
}
