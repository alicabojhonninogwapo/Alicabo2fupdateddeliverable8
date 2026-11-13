package peerpal.model;

/**
 * Immutable data model for a row in the tutoring_sessions table.
 * Tutor and student usernames are joined in for display purposes.
 */
public record TutoringSession(int id, int studentId, String studentName,
                              int tutorId, String tutorName,
                              String subject, String status) {
}
