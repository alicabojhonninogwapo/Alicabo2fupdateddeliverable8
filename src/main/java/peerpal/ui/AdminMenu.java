package peerpal.ui;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.model.ActivityLog;
import peerpal.model.Role;
import peerpal.model.User;
import peerpal.ui.RoleMenu;

import java.util.List;
import java.util.Scanner;

/**
 * Dashboard shown to users with the ADMIN role after successful login.
 * Lets the administrator review accounts, approve pending registrations,
 * update the role and status of other users, and inspect the system audit trail.
 */
public class AdminMenu extends RoleMenu {

    /** Statuses an account is allowed to have. */
    private static final String[] VALID_STATUSES = {"APPROVED", "PENDING"};

    public AdminMenu(User user, Scanner scanner, UserDao userDao,
                     ActivityLogDao logDao, TutoringSessionDao sessionDao) {
        super(user, scanner, userDao, logDao, sessionDao);
    }

    @Override
    protected String title() {
        return "Admin";
    }

    @Override
    public void show() {
        System.out.println("\nLogin successful! Welcome, " + user.username() + " (Admin).");

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println();
            System.out.println("--- Admin Dashboard ---");
            System.out.println("1. View all system users");
            System.out.println("2. View pending registrations");
            System.out.println("3. Approve user account");
            System.out.println("4. Update user role and status");
            System.out.println("5. View system audit logs");
            System.out.println("6. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> updateUser();
                case "5" -> auditLogMenu();
                case "6" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void viewAllUsers() {
        System.out.println("\n--- All System Users ---");
        printUserTable(userDao.getAllUsers());
    }

    private void viewPendingUsers() {
        System.out.println("\n--- Pending Registrations ---");
        printUserTable(userDao.getPendingUsers());
    }

    /**
     * Shows pending accounts, asks for a user ID, checks it is in the
     * pending list, then approves it. Entering 0 cancels.
     */
    private void approveUser() {
        System.out.println("\n--- Approve User Account ---");
        List<User> pending = userDao.getPendingUsers();
        printUserTable(pending);

        if (pending.isEmpty()) {
            return;
        }

        System.out.print("Enter the User ID to approve (0 to cancel): ");
        String input = scanner.nextLine().trim();

        int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric User ID.");
            return;
        }

        if (targetId == 0) {
            System.out.println("Approval cancelled.");
            return;
        }

        boolean inPendingList = pending.stream().anyMatch(u -> u.id() == targetId);
        if (!inPendingList) {
            System.out.println("User ID " + targetId + " is not in the pending list.");
            return;
        }

        if (userDao.approveUser(targetId, user.id())) {
            System.out.println("User ID " + targetId + " has been APPROVED.");
        } else {
            System.out.println("Approval failed. Please try again.");
        }
    }

    /**
     * Lets the admin change another user's role and status.
     * Steps: show the table -> ask for the User ID -> ask for the new role
     * -> ask for the new status -> save. Entering 0 at any step cancels.
     */
    private void updateUser() {
        System.out.println("\n--- Update User Role and Status ---");
        List<User> users = userDao.getAllUsers();
        printUserTable(users);

        if (users.isEmpty()) {
            return;
        }

        // Step 1: pick the target user (re-ask until the ID is acceptable)
        User target = null;
        while (target == null) {
            int targetUserId = readIntInput("Enter the User ID to update (0 to cancel): ");

            if (targetUserId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            // Self-edit restriction: an admin cannot change their own active account
            if (targetUserId == user.id()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }

            for (User u : users) {
                if (u.id() == targetUserId) {
                    target = u;
                    break;
                }
            }
            if (target == null) {
                System.out.println("User ID " + targetUserId + " does not exist. Please try again.");
            }
        }

        // Step 2: new role
        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 3: new status
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 4: save to the database
        if (userDao.updateUserRoleAndStatus(target.id(), newRole, newStatus, user.id())) {
            System.out.println("SUCCESS: User '" + target.username() + "' (ID " + target.id()
                    + ") is now " + newRole + " / " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    // ------------------------------------------------------------------
    // Audit log (activity_logs)
    // ------------------------------------------------------------------

    /** Sub-menu: view all logs, filter by user ID, or clear the log history. */
    private void auditLogMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("--- System Audit Logs ---");
            System.out.println("1. View all system logs");
            System.out.println("2. Filter logs by User ID");
            System.out.println("3. Clear all logs");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printLogTable(logDao.getAllLogs());
                case "2" -> filterLogsByUser();
                case "3" -> clearLogs();
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /** Asks for a user ID (re-prompts on non-numeric input; 0 cancels) and shows that user's logs. */
    private void filterLogsByUser() {
        int userId = readIntInput("Enter the User ID to filter by (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        System.out.println("\n--- Logs for User ID " + userId + " ---");
        printLogTable(logDao.getLogsByUserId(userId));
    }

    /**
     * Deletes the whole log history, but only after the admin types YES.
     * The clearing itself is then recorded as a fresh LOGS_CLEARED entry,
     * so the audit trail is never silently emptied.
     */
    private void clearLogs() {
        int total = logDao.getAllLogs().size();
        if (total == 0) {
            System.out.println("There are no logs to clear.");
            return;
        }

        System.out.println("WARNING: This permanently deletes all " + total + " log entries.");
        System.out.print("Type YES to confirm (anything else cancels): ");
        if (!scanner.nextLine().trim().equals("YES")) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }

        int deleted = logDao.clearAllLogs();
        logDao.logAction(user.id(), "LOGS_CLEARED",
                "Admin [" + user.id() + "] cleared " + deleted + " log entries.");
        System.out.println(deleted + " log entries cleared.");
    }

    /**
     * Prints logs in an aligned console table. A description longer than its
     * column is word-wrapped onto extra lines so the columns always stay aligned.
     */
    static void printLogTable(List<ActivityLog> logs) {
        final int descWidth = 35;
        String rowFormat = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        String line = "-".repeat(5) + "-+-" + "-".repeat(8) + "-+-" + "-".repeat(18)
                + "-+-" + "-".repeat(descWidth) + "-+-" + "-".repeat(20);

        System.out.println();
        System.out.printf(rowFormat, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(line);

        if (logs.isEmpty()) {
            System.out.println("No log entries found.");
        } else {
            for (ActivityLog log : logs) {
                List<String> parts = wrap(log.description(), descWidth);
                System.out.printf(rowFormat, log.id(), log.userId(), log.actionType(),
                        parts.get(0), log.timestamp());
                // Continuation lines for long descriptions
                for (int i = 1; i < parts.size(); i++) {
                    System.out.printf(rowFormat, "", "", "", parts.get(i), "");
                }
            }
        }
        System.out.println(line);
        System.out.println("Total: " + logs.size());
    }

    /** Splits text into lines of at most 'width' characters, breaking at spaces where possible. */
    private static List<String> wrap(String text, int width) {
        List<String> lines = new java.util.ArrayList<>();
        String rest = text.trim();
        while (rest.length() > width) {
            int cut = rest.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width; // no space to break at: hard cut
            }
            lines.add(rest.substring(0, cut));
            rest = rest.substring(cut).trim();
        }
        lines.add(rest);
        return lines;
    }

    // ------------------------------------------------------------------
    // Input validation wrappers
    // ------------------------------------------------------------------

    /**
     * Asks for a role until a valid one (see the Role enum, e.g. STUDENT,
     * TEACHER, TUTOR, ADMIN) is typed. Not case-sensitive.
     * Returns the role in UPPERCASE, or null if the admin entered 0 to cancel.
     */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role (" + joinRoles() + ", or 0 to cancel): ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }

            var role = Role.fromString(input);
            if (role.isPresent()) {
                return role.get().name();
            }
            System.out.println("Invalid role. Please choose one of: " + joinRoles() + ".");
        }
    }

    /**
     * Asks for a status until APPROVED or PENDING is typed. Not case-sensitive.
     * Returns the status in UPPERCASE, or null if the admin entered 0 to cancel.
     */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (" + String.join(", ", VALID_STATUSES) + ", or 0 to cancel): ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }

            for (String status : VALID_STATUSES) {
                if (status.equalsIgnoreCase(input)) {
                    return status;
                }
            }
            System.out.println("Invalid status. Please choose one of: "
                    + String.join(", ", VALID_STATUSES) + ".");
        }
    }

    /** Builds "TEACHER, STUDENT, TUTOR, ADMIN" from the Role enum. */
    private static String joinRoles() {
        StringBuilder sb = new StringBuilder();
        for (Role r : Role.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r.name());
        }
        return sb.toString();
    }

    /**
     * Prints any list of users as an aligned table with borders.
     * Passwords are intentionally never displayed.
     */
    static void printUserTable(List<User> users) {
        String rowFormat = "| %-5s | %-15s | %-12s | %-10s |%n";
        String border = "+" + "-".repeat(7) + "+" + "-".repeat(17)
                + "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+";

        System.out.println(border);
        System.out.printf(rowFormat, "ID", "Username", "Role", "Status");
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.printf("| %-51s |%n", "No users found.");
        } else {
            for (User u : users) {
                System.out.printf(rowFormat, u.id(), u.username(), u.roles(), u.status());
            }
        }
        System.out.println(border);
        System.out.println("Total: " + users.size());
    }
}
