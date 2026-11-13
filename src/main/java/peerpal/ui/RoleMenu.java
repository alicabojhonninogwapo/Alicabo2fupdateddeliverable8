package peerpal.ui;

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

    protected RoleMenu(User user, Scanner scanner, UserDao userDao) {
        this.user = user;
        this.scanner = scanner;
        this.userDao = userDao;
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
                case "1" -> {
                    System.out.println("\n--- Profile ---");
                    System.out.println("ID       : " + user.id());
                    System.out.println("Username : " + user.username());
                    System.out.println("Role     : " + user.roles());
                    System.out.println("Status   : " + user.status());
                }
                case "2" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
