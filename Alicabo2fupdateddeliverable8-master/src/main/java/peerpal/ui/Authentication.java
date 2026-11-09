package peerpal.ui;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.model.Role;
import peerpal.model.User;
import peerpal.ui.AdminMenu;
import peerpal.ui.StudentMenu;
import peerpal.ui.TeacherMenu;
import peerpal.ui.TutorMenu;

import java.util.Optional;
import java.util.Scanner;

/**
 * Handles registration, login, and role-based routing.
 * Single shared Scanner and UserDao are passed from Main to avoid
 * creating duplicate resources.
 */
public class Authentication {

    private final Scanner scanner;
    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final TutoringSessionDao sessionDao;

    public Authentication(Scanner scanner, UserDao userDao,
                          ActivityLogDao logDao, TutoringSessionDao sessionDao) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.logDao = logDao;
        this.sessionDao = sessionDao;
    }

    /**
     * Main loop: Register / Login / Exit.
     * Called once from Main after database initialization.
     */
    public void start() {
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("=================================");
            System.out.println("  PeerPal - Peer Tutoring System");
            System.out.println("=================================");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "0" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- Register Account ---");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty() || username.contains(" ")) {
            System.out.println("Username cannot be empty or contain spaces.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("That username is already taken.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        if (password.isBlank()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        // Role selection - built from the Role enum so every system role is supported
        Role[] roles = Role.values();
        System.out.println("Select role:");
        for (int i = 0; i < roles.length; i++) {
            System.out.println((i + 1) + ". " + roles[i]);
        }
        System.out.print("Choose: ");
        Role selected = null;
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= roles.length) {
                selected = roles[choice - 1];
            }
        } catch (NumberFormatException ignored) {
            // falls through to the invalid-role message below
        }
        if (selected == null) {
            System.out.println("Invalid role. Registration cancelled.");
            return;
        }
        String role = selected.name();

        if (userDao.register(username, password, role)) {
            System.out.println("Account created! Role: " + role + " | Status: PENDING");
            System.out.println("(Your account is waiting for approval.)");
        } else {
            System.out.println("Registration failed. Username may already exist.");
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.login(username, password);

        if (result.isPresent()) {
            User user = result.get();
            logDao.logAction(user.id(), "USER_LOGIN",
                    "User " + user.username() + " (ID: " + user.id() + ") logged in.");
            // Route to the correct role-based menu; returns when the user logs out
            routeByRole(user);
            logout(user);
        } else {
            System.out.println("Invalid username or password.");
        }
    }

    /** Records the logout once the user's dashboard has closed. */
    private void logout(User user) {
        logDao.logAction(user.id(), "USER_LOGOUT",
                "User " + user.username() + " (ID: " + user.id() + ") logged out.");
    }

    /**
     * Sends the logged-in user to the appropriate dashboard
     * based on their system role.
     */
    private void routeByRole(User user) {
        Optional<Role> role = Role.fromString(user.roles());
        if (role.isEmpty()) {
            System.out.println("Unknown role: " + user.roles() + ". Logging out.");
            return;
        }
        switch (role.get()) {
            case TEACHER -> new TeacherMenu(user, scanner, userDao, logDao, sessionDao).show();
            case STUDENT -> new StudentMenu(user, scanner, userDao, logDao, sessionDao).show();
            case TUTOR -> new TutorMenu(user, scanner, userDao, logDao, sessionDao).show();
            case ADMIN -> new AdminMenu(user, scanner, userDao, logDao, sessionDao).show();
        }
    }
}
