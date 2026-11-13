package peerpal.ui;

import peerpal.dao.UserDao;
import peerpal.model.User;

import java.util.Scanner;

/**
 * Dashboard shown to users with the TUTOR role after successful login.
 */
public class TutorMenu extends RoleMenu {

    public TutorMenu(User user, Scanner scanner, UserDao userDao) {
        super(user, scanner, userDao);
    }

    @Override
    protected String title() {
        return "Tutor";
    }
}
