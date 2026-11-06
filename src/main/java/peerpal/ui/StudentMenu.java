package peerpal.ui;

import peerpal.dao.UserDao;
import peerpal.model.User;

import java.util.Scanner;

/**
 * Dashboard shown to users with the STUDENT role after successful login.
 */
public class StudentMenu extends RoleMenu {

    public StudentMenu(User user, Scanner scanner, UserDao userDao) {
        super(user, scanner, userDao);
    }

    @Override
    protected String title() {
        return "Student";
    }
}
