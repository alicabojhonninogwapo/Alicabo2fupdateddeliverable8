package peerpal;

import peerpal.dao.DatabaseInitializer;
import peerpal.dao.UserDao;
import peerpal.ui.Authentication;

import java.util.Scanner;

/**
 * Single application entry point.
 *
 * Execution flow:
 *   1. Initialize the database (create tables if needed)
 *   2. Create shared DAO and Scanner instances
 *   3. Delegate all UI / authentication logic to Authentication.start()
 */
public class Main {

    public static void main(String[] args) {
        // 1. Guard: ensure all tables exist before anything else runs
        DatabaseInitializer.initialize();

        // 2. Shared resources (one Scanner, one UserDao for the whole app)
        Scanner scanner = new Scanner(System.in);
        UserDao userDao = new UserDao();

        // 3. Hand control to the Authentication UI
        new Authentication(scanner, userDao).start();

        scanner.close();
    }
}
