/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import play.*;
import play.cache.*;
import play.mvc.*;

import java.util.*;

import models.*;

/**
 *
 * @author demory
 */

@With(Secure.class)
public class Admin extends Controller {
  
    public static void getActiveUsers() {
        List<TrinetUser> activeUsers = new ArrayList();
        List<TrinetUser> allUsers = TrinetUser.findAll();
        for (TrinetUser user : allUsers) {
            if (user.isActive()) activeUsers.add(user);
        }
        render(activeUsers);
    }

    public static void getInactiveUsers() {
        List<TrinetUser> inactiveUsers = new ArrayList();
        List<TrinetUser> allUsers = TrinetUser.findAll();
        for (TrinetUser user : allUsers) {
            if (!user.isActive()) inactiveUsers.add(user);
        }
        render(inactiveUsers);
    }

    public static void addUser(TrinetUser user) {
        user.save();
        getActiveUsers();
    }
    
    public static void deleteUser(long id) {
        TrinetUser user = TrinetUser.findById(id);
       
        List<Session> sessions;

        sessions = Session.find("byUser", user).fetch();
        for(Session sess : sessions) {
            sess.delete();
        }
       
        user.delete();
        getActiveUsers();
    }

    public static void deactivateUser(long id) {
        TrinetUser user = TrinetUser.findById(id);

        List<Session> sessions;

        sessions = Session.find("byUser", user).fetch();
        for(Session sess : sessions) {
            sess.delete();
        }

        user.status = "inactive";
        user.save();
        getActiveUsers();
    }

    public static void reactivateUser(long id) {
        TrinetUser user = TrinetUser.findById(id);

        user.status = "active";
        user.save();
        getActiveUsers();
    }

    public static void changeUserRole(long id, String role) {
        TrinetUser user = TrinetUser.findById(id);
        user.role = role;
        user.save();
        getActiveUsers();
    }
    
}
