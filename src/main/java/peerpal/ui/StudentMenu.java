package peerpal.ui;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.model.TutoringSession;
import peerpal.model.User;
import peerpal.ui.AdminMenu;
import peerpal.ui.RoleMenu;

import java.util.List;
import java.util.Scanner;

/**
 * Dashboard shown to users with the STUDENT role after successful login.
 * Students can book tutoring sessions (logged as SESSION_BOOKED) and
 * follow their status.
 */
public class StudentMenu extends RoleMenu {

    public StudentMenu(User user, Scanner scanner, UserDao userDao,
                       ActivityLogDao logDao, TutoringSessionDao sessionDao) {
        super(user, scanner, userDao, logDao, sessionDao);
    }

    @Override
    protected String title() {
        return "Student";
    }

    @Override
    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username() + " (" + title() + ").");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("--- Student Dashboard ---");
            System.out.println("Status   : " + user.status());
            if ("PENDING".equals(user.status())) {
                System.out.println("(Your account is still waiting for approval.)");
            }
            System.out.println();
            System.out.println("1. View my profile");
            System.out.println("2. Book a tutoring session");
            System.out.println("3. View my sessions");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printProfile();
                case "2" -> bookSession();
                case "3" -> printSessionTable(sessionDao.getSessionsByStudent(user.id()));
                case "4" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /** Shows approved tutors, asks for a tutor ID and a subject, then books the session. 0 cancels. */
    private void bookSession() {
        if (!"APPROVED".equals(user.status())) {
            System.out.println("Your account must be APPROVED before you can book a session.");
            return;
        }

        System.out.println("\n--- Book a Tutoring Session ---");
        List<User> tutors = userDao.getApprovedUsersByRole("TUTOR");
        AdminMenu.printUserTable(tutors);
        if (tutors.isEmpty()) {
            System.out.println("There are no approved tutors yet.");
            return;
        }

        User tutor = null;
        while (tutor == null) {
            int tutorId = readIntInput("Enter the Tutor ID (0 to cancel): ");
            if (tutorId == 0) {
                System.out.println("Booking cancelled.");
                return;
            }
            for (User t : tutors) {
                if (t.id() == tutorId) {
                    tutor = t;
                    break;
                }
            }
            if (tutor == null) {
                System.out.println("Tutor ID " + tutorId + " is not in the list. Please try again.");
            }
        }

        String subject;
        while (true) {
            System.out.print("Subject to be tutored (0 to cancel): ");
            subject = scanner.nextLine().trim();
            if (subject.equals("0")) {
                System.out.println("Booking cancelled.");
                return;
            }
            if (!subject.isEmpty()) {
                break;
            }
            System.out.println("Subject cannot be empty.");
        }

        int sessionId = sessionDao.bookSession(user.id(), tutor.id(), tutor.username(), subject);
        if (sessionId > 0) {
            System.out.println("Session #" + sessionId + " booked with " + tutor.username() + ". Status: PENDING");
        } else {
            System.out.println("Booking failed. Please try again.");
        }
    }

    /** Prints sessions as an aligned table (shared with TutorMenu). */
    static void printSessionTable(List<TutoringSession> sessions) {
        String rowFormat = "| %-5s | %-15s | %-15s | %-20s | %-10s |%n";
        String border = "+" + "-".repeat(7) + "+" + "-".repeat(17) + "+" + "-".repeat(17)
                + "+" + "-".repeat(22) + "+" + "-".repeat(12) + "+";

        System.out.println();
        System.out.println(border);
        System.out.printf(rowFormat, "ID", "Student", "Tutor", "Subject", "Status");
        System.out.println(border);
        if (sessions.isEmpty()) {
            System.out.printf("| %-80s |%n", "No sessions found.");
        } else {
            for (TutoringSession s : sessions) {
                String subject = s.subject().length() > 20 ? s.subject().substring(0, 17) + "..." : s.subject();
                System.out.printf(rowFormat, s.id(), s.studentName(), s.tutorName(), subject, s.status());
            }
        }
        System.out.println(border);
        System.out.println("Total: " + sessions.size());
    }
}
