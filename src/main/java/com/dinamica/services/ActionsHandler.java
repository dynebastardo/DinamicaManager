package com.dinamica.services;

import com.google.gson.JsonObject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.servlet.http.HttpServletRequest;

public class ActionsHandler {
    
    final private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ActionsHandler.class);
    
    public JsonObject run(String action, HttpServletRequest request) {
        //Switches between values of action= parameter
        switch(action) {
            case "login"         : return login(action, request);
            case "userparameters": return userParameters(action, request);
            case "createuser"    : return createUser(action, request);
            default              : return defReturn(action);
        }
    }
    
    //Accomplishes Login
    private JsonObject login(String action, HttpServletRequest req) {
        JsonObject json = new JsonObject();
        json.addProperty("result", "true");
        json.addProperty("action", action);
        return json;
    }
    
    // Returns array of all user parameters
    private JsonObject userParameters(String action, HttpServletRequest req) {
        LDAPUtils ldap = new LDAPUtils();        
        NamingEnumeration nam = ldap.searchDomain();
        return Utils.nEnumToJSON(nam);
    }
    
    //Creates a User
    private JsonObject createUser(String action, HttpServletRequest req) {
        JsonObject json = new JsonObject();
        final String parameter = req.getParameter("userInfo");
        final String[] info = parameter.split(",");
        //Sets the mandatory parameters:
        // 0 - First Name
        // 1 - Last Name
        // 2 - User Id 
        // 3 - Password
        LDAPUtils ldap = new LDAPUtils();
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
        
        //Validate if DN already exists
        try {
            final NamingEnumeration nEnum = ldap.searchDomain(new String[]{"distinguishedName"},null,ldap.getUserDN());
            final Attributes attrs = ldap.singleSearch(nEnum);
            final String dn = attrs.get("distinguishedName").get().toString();
            if (dn.equalsIgnoreCase(ldap.getUserDN())) {
                log.error("Pirulito");
                json.addProperty("result", "duplicateDN");
                return json;
            }
        } catch (NamingException|NullPointerException ex) {
            log.info(ex);
            //Ignore
        }
        //Validate if UserID already used
        try {
            String filter = new StringBuffer().append("(sAMAccountName=").append(ldap.getUserId()).append(")").toString();
            final NamingEnumeration nEnum = ldap.searchDomain(new String[]{"sAMAccountName"},filter);
            final Attributes attrs = ldap.singleSearch(nEnum);
            final String uid = attrs.get("sAMAccountName").get().toString();
            if (uid.equalsIgnoreCase(ldap.getUserId())) {
                json.addProperty("result", "duplicateUID");
                return json;
            }
        } catch (NamingException|NullPointerException ex) {
            log.info(ex);
            //Ignore
        }
        ldap.addUser();
        json.addProperty("result", "success");
        return json;
    }
    
    private JsonObject defReturn(String action) {
        JsonObject json = new JsonObject();
        json.addProperty("result", "error"); 
        json.addProperty("action", action); 
        json.addProperty("message", "Action not found"); 
        return json;
    }
}
