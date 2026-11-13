package peerpal.ui;

import peerpal.dao.UserDao;
import peerpal.model.User;

import java.util.Scanner;

/**
 * Dashboard shown to users with the TEACHER role after successful login.
 */
public class TeacherMenu extends RoleMenu {

    public TeacherMenu(User user, Scanner scanner, UserDao userDao) {
        super(user, scanner, userDao);
    }

    @Override
    protected String title() {
        return "Teacher";
    }
}
