package peerpal.ui;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.model.TutoringSession;
import peerpal.model.User;
import peerpal.ui.RoleMenu;
import peerpal.ui.StudentMenu;

import java.util.List;
import java.util.Scanner;

/**
 * Dashboard shown to users with the TUTOR role after successful login.
 * Tutors can review the sessions booked with them and move each one through
 * its lifecycle (logged as SESSION_UPDATED).
 */
public class TutorMenu extends RoleMenu {

    /** Statuses a tutor may set on a session. */
    private static final String[] SESSION_STATUSES = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"};

    public TutorMenu(User user, Scanner scanner, UserDao userDao,
                     ActivityLogDao logDao, TutoringSessionDao sessionDao) {
        super(user, scanner, userDao, logDao, sessionDao);
    }

    @Override
    protected String title() {
        return "Tutor";
    }

    @Override
    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username() + " (" + title() + ").");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("--- Tutor Dashboard ---");
            System.out.println("Status   : " + user.status());
            if ("PENDING".equals(user.status())) {
                System.out.println("(Your account is still waiting for approval.)");
            }
            System.out.println();
            System.out.println("1. View my profile");
            System.out.println("2. View my tutoring sessions");
            System.out.println("3. Update session status");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printProfile();
                case "2" -> StudentMenu.printSessionTable(sessionDao.getSessionsByTutor(user.id()));
                case "3" -> updateSessionStatus();
                case "4" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /** Shows the tutor's sessions, asks for a session ID and a new status. 0 cancels. */
    private void updateSessionStatus() {
        System.out.println("\n--- Update Session Status ---");
        List<TutoringSession> sessions = sessionDao.getSessionsByTutor(user.id());
        StudentMenu.printSessionTable(sessions);
        if (sessions.isEmpty()) {
            return;
        }

        TutoringSession target = null;
        while (target == null) {
            int sessionId = readIntInput("Enter the Session ID to update (0 to cancel): ");
            if (sessionId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            for (TutoringSession s : sessions) {
                if (s.id() == sessionId) {
                    target = s;
                    break;
                }
            }
            if (target == null) {
                System.out.println("Session ID " + sessionId + " is not in your list. Please try again.");
            }
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (sessionDao.updateStatus(target.id(), user.id(), newStatus)) {
            System.out.println("SUCCESS: Session #" + target.id() + " is now " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    /** Asks for a session status until a valid one is typed. Returns it in UPPERCASE, or null for 0. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (" + String.join(", ", SESSION_STATUSES) + ", or 0 to cancel): ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }
            for (String status : SESSION_STATUSES) {
                if (status.equalsIgnoreCase(input)) {
                    return status;
                }
            }
            System.out.println("Invalid status. Please choose one of: " + String.join(", ", SESSION_STATUSES) + ".");
        }
    }
}
