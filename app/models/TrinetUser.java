/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package models;

import javax.persistence.Entity;
import play.db.jpa.*;

/**
 *
 * @author demory
 */

@Entity
public class TrinetUser extends Model {
  
    public String username;
    
    public String role;

    public String status = "active"; // "active" or "inactive"
    
    @Override
    public String toString() {
        return String.format("TrinetUser %s (%s)", username, role);
    }

    public boolean isActive() {
        // assume that a null status indicates an active user
        // (i.e. one from before when the status field was implemented)
        return status == null || status.equals("active");
    }

    public boolean hasCalltakerAccess() {
        return isActive() && (role.equals("calltaker") || role.equals("all"));
    }

    public boolean hasFieldTripAccess() {
        return isActive() && (role.equals("fieldtrip") || role.equals("all"));
    }
}
