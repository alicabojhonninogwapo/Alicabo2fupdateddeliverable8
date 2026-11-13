package peerpal.model;

/**
 * Immutable data model for a row in the users table.
 * Java record keeps the class concise and thread-safe.
 */
public record User(int id, String username, String password, String roles, String status) {
}
