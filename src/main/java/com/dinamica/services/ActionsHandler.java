package com.dinamica.services;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class ActionsHandler {
    
    private LDAPUtils ldap;
    private HttpServletRequest req;
    
    public ActionsHandler(LDAPUtils param) {
        // Receive LDAP Context from parent
        ldap = param;
    }
    
    public JSONObject run(String action, HttpServletRequest request) {
        req = request;
        //Switches between values of action= parameter
        switch(action) {
            case "userParameters": return userParameters();
            case "createUser":     return createUser();
            default: return null;
        }
    }
    
    // Returns array of all user parameters
    private JSONObject userParameters() {
        //Gets User id
        final String userId = req.getParameter("uid");
        //Gets User's Organization Unit
        final String orgUnitStr = req.getParameter("ou");
        JSONObject json = new JSONObject();
        //Sets User Id for LDAP Conext
        ldap.setUserId(userId);
        // If not blank, sets User Organization Unit for LDAP Conext
        if (orgUnitStr != null && !orgUnitStr.isEmpty()) {
            ldap.setOrgUnitStr(orgUnitStr);
        }
        //Loops through all User Attributes and spools to JSONObject
        try {
            for (NamingEnumeration ae = ldap.getAttrs().getAll(); ae.hasMore();) {
                final Attribute attr = (Attribute) ae.next();
                for (NamingEnumeration e = attr.getAll(); e.hasMore(); e.toString()) {
                    //If the Attribute is userPassword, tries do decode
                    if (attr.getID().equals("userPassword")) {
                        final String pwd = new String((byte[]) e.next());
                        json.put(attr.getID(), pwd);
                      } else {
                    //If it's not the password, pass the pure Attribute
                        json.put(attr.getID(), e.next());
                      }   
                }
            }
        } catch (NamingException ex1) {
            Logger.getLogger(JsonParserServlet.class.getName()).log(Level.SEVERE, null, ex1);
            return null;
        }
        return json;
    }
    
    //Creates a User
    private JSONObject createUser() {
        JSONObject json = new JSONObject();
        final String parameter = req.getParameter("userInfo");
        final String[] info = parameter.split(",");
        //Sets the mandatory parameters:
        // 0 - First Name
        // 1 - Last Name
        // 2 - User Id 
        // 3 - Password
        if (info.length >= 4) {
            ldap.setUserFirstName(info[0]);
            ldap.setUserLastName(info[1]);
            ldap.setUserId(info[2]);
            ldap.setUserPass(info[3]);
        }
        
        //Sets optional Parameter
        // 4 - Organization Unit
        if (info.length >=5) {
            ldap.setOrgUnitStr(info[4]);
        } else {
            ldap.setDefaultOrgUnitStr();
        }
            
        
        try {
            final String[] srchResult = {"uid"};
            final Attributes attr = ldap.searchUser(srchResult);
            String uid = new String();
            if (attr != null) {
                uid = attr.get("uid").toString();
                uid = uid.substring(uid.indexOf(":") + 1).trim();
            }
            if (!uid.equals(ldap.getUserId())){
                ldap.addUser();
                return json.put("result", "success");
            } else {
                return json.put("result", "duplicate");
            }                
        } catch (NamingException ex) {
            Logger.getLogger(ActionsHandler.class.getName()).log(Level.SEVERE, null, ex);
            return json.put("result", "error");
        }
    }
}
