package peerpal.dao;

import peerpal.dao.DatabaseConnection;
import peerpal.model.TutoringSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL queries related to the tutoring_sessions table.
 * This is the PeerPal "core business process": a student books a session
 * with a tutor, and the tutor moves it through its lifecycle.
 * Both actions are written to the audit trail.
 */
public class TutoringSessionDao {

    private static final String SELECT_SESSIONS =
            "SELECT s.id, s.student_id, st.username AS student_name, "
                    + "s.tutor_id, tu.username AS tutor_name, s.subject, s.status "
                    + "FROM tutoring_sessions s "
                    + "JOIN users st ON st.id = s.student_id "
                    + "JOIN users tu ON tu.id = s.tutor_id ";

    private final ActivityLogDao logDao;

    public TutoringSessionDao(ActivityLogDao logDao) {
        this.logDao = logDao;
    }

    /**
     * Core business process: a student books a session with a tutor.
     * The new session starts as PENDING. Returns the new session id, or -1 on failure.
     * Logs SESSION_BOOKED.
     */
    public int bookSession(int studentId, int tutorId, String tutorName, String subject) {
        String sql = "INSERT INTO tutoring_sessions (student_id, tutor_id, subject, status) "
                + "VALUES (?, ?, ?, 'PENDING')";
        int newId = -1;

        try (Connection conn = peerpal.dao.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, tutorId);
            ps.setString(3, subject);
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Booking failed: " + e.getMessage(), e);
        }

        // Log after the connection is closed so the two connections never overlap
        if (newId > 0) {
            logDao.logAction(studentId, "SESSION_BOOKED",
                    "Booked session #" + newId + " with Tutor " + tutorName
                            + " (ID: " + tutorId + ") - Subject: " + subject);
        }
        return newId;
    }

    /**
     * Lifecycle / state change: the tutor changes the status of one of their own sessions.
     * Returns true only if that session belongs to the tutor and was updated.
     * Logs SESSION_UPDATED, e.g. "Updated Session #3 status from PENDING to CONFIRMED".
     */
    public boolean updateStatus(int sessionId, int tutorId, String newStatus) {
        Optional<String> oldStatus = findStatus(sessionId, tutorId);
        if (oldStatus.isEmpty()) {
            return false;
        }

        String sql = "UPDATE tutoring_sessions SET status = ? WHERE id = ? AND tutor_id = ?";
        boolean updated;

        try (Connection conn = peerpal.dao.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.toUpperCase());
            ps.setInt(2, sessionId);
            ps.setInt(3, tutorId);
            updated = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Status update failed: " + e.getMessage(), e);
        }

        if (updated) {
            logDao.logAction(tutorId, "SESSION_UPDATED",
                    "Updated Session #" + sessionId + " status from " + oldStatus.get()
                            + " to " + newStatus.toUpperCase());
        }
        return updated;
    }

    /** Sessions booked by one student, newest first. */
    public List<TutoringSession> getSessionsByStudent(int studentId) {
        return querySessions(SELECT_SESSIONS + "WHERE s.student_id = ? ORDER BY s.id DESC", studentId);
    }

    /** Sessions assigned to one tutor, newest first. */
    public List<TutoringSession> getSessionsByTutor(int tutorId) {
        return querySessions(SELECT_SESSIONS + "WHERE s.tutor_id = ? ORDER BY s.id DESC", tutorId);
    }

    /** Current status of a session, only if it belongs to the given tutor. */
    private Optional<String> findStatus(int sessionId, int tutorId) {
        String sql = "SELECT status FROM tutoring_sessions WHERE id = ? AND tutor_id = ?";

        try (Connection conn = peerpal.dao.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, tutorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("status")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lookup failed: " + e.getMessage(), e);
        }
    }

    private List<TutoringSession> querySessions(String sql, int userId) {
        List<TutoringSession> sessions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(new TutoringSession(
                            rs.getInt("id"),
                            rs.getInt("student_id"),
                            rs.getString("student_name"),
                            rs.getInt("tutor_id"),
                            rs.getString("tutor_name"),
                            rs.getString("subject"),
                            rs.getString("status")));
                }
            }
            return sessions;
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }
}
