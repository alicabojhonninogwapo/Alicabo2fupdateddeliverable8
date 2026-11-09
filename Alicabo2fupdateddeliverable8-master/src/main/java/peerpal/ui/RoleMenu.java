package peerpal.ui;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.model.User;

import java.util.Scanner;

/**
 * Base class for every role-based dashboard.
 * Receives the single shared Scanner and UserDao created in Main.
 * Subclasses only supply the role title; add role-specific options later.
 */
public abstract class RoleMenu {

    protected final User user;
    protected final Scanner scanner;
    protected final UserDao userDao;
    protected final ActivityLogDao logDao;
    protected final TutoringSessionDao sessionDao;

    protected RoleMenu(User user, Scanner scanner, UserDao userDao,
                       ActivityLogDao logDao, TutoringSessionDao sessionDao) {
        this.user = user;
        this.scanner = scanner;
        this.userDao = userDao;
        this.logDao = logDao;
        this.sessionDao = sessionDao;
    }

    /** Display name of the dashboard, e.g. "Teacher". */
    protected abstract String title();

    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username() + " (" + title() + ").");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("--- " + title() + " Dashboard ---");
            System.out.println("Username : " + user.username());
            System.out.println("Role     : " + user.roles());
            System.out.println("Status   : " + user.status());
            if ("PENDING".equals(user.status())) {
                System.out.println("(Your account is still waiting for approval.)");
            }
            System.out.println();
            System.out.println("1. View my profile");
            System.out.println("2. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printProfile();
                case "2" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /** Prints the logged-in user's profile. */
    protected void printProfile() {
        System.out.println("\n--- Profile ---");
        System.out.println("ID       : " + user.id());
        System.out.println("Username : " + user.username());
        System.out.println("Role     : " + user.roles());
        System.out.println("Status   : " + user.status());
    }

    /**
     * Asks for a whole number and keeps asking until one is typed.
     * Letters or symbols never crash the program; they just re-prompt.
     */
    protected int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (digits only).");
            }
        }
    }
}
