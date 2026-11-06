package peerpal.model;

/**
 * Immutable data model for a row in the activity_logs table.
 * The timestamp is kept as the text SQLite stores (yyyy-MM-dd HH:mm:ss).
 */
public record ActivityLog(int id, int userId, String actionType, String description, String timestamp) {
}
