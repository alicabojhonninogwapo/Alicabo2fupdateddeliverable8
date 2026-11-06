package peerpal;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.DatabaseInitializer;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.ui.Authentication;

import java.util.Scanner;

/**
 * Single application entry point.
 *
 * Execution flow:
 *   1. Initialize the database (create tables if needed)
 *   2. Create the shared DAO and Scanner instances
 *   3. Delegate all UI / authentication logic to Authentication.start()
 */
public class Main {

    public static void main(String[] args) {
        // 1. Guard: ensure all tables exist before anything else runs
        DatabaseInitializer.initialize();

        // 2. Shared resources (one Scanner and one of each DAO for the whole app).
        //    The ActivityLogDao is passed into every DAO that must write to the audit trail.
        Scanner scanner = new Scanner(System.in);
        ActivityLogDao logDao = new ActivityLogDao();
        UserDao userDao = new UserDao(logDao);
        TutoringSessionDao sessionDao = new TutoringSessionDao(logDao);

        // 3. Hand control to the Authentication UI
        new Authentication(scanner, userDao, logDao, sessionDao).start();

        scanner.close();
    }
}
