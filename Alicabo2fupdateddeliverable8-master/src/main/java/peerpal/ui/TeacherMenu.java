package peerpal.ui;

import peerpal.dao.ActivityLogDao;
import peerpal.dao.TutoringSessionDao;
import peerpal.dao.UserDao;
import peerpal.model.User;
import peerpal.ui.RoleMenu;

import java.util.Scanner;

/**
 * Dashboard shown to users with the TEACHER role after successful login.
 */
public class TeacherMenu extends RoleMenu {

    public TeacherMenu(User user, Scanner scanner, UserDao userDao,
                       ActivityLogDao logDao, TutoringSessionDao sessionDao) {
        super(user, scanner, userDao, logDao, sessionDao);
    }

    @Override
    protected String title() {
        return "Teacher";
    }
}
