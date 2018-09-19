package com.dinamica.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

class LDAPUtils {
    
    final private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LDAPUtils.class);
    
    final String conntype  = "simple";
    final String ldapCtxt  = "com.sun.jndi.ldap.LdapCtxFactory";
    final int UF_ACCOUNTDISABLE = 0x0002;
    final int UF_PASSWD_NOTREQD = 0x0020;
    final int UF_PASSWD_CANT_CHANGE = 0x0040;
    final int UF_NORMAL_ACCOUNT = 0x0200;
    final int UF_DONT_EXPIRE_PASSWD = 0x10000;
    final int UF_PASSWORD_EXPIRED = 0x800000;
    final int NOT_CHANGE_PASS_LOGON = 512;
    
    private String url;
    private String domainStr;
    private String orgUnitStr;
    private String defaultOrgUnitStr;
    private String mail;
    private String adminName;
    private String adminPwd;
    private String userId;
    private String userPass;
    private String userFirstName; 
    private String userLastName;
    
    public LDAPUtils() {
        final Properties prop = new Properties();        
        try {
            prop.load(ContextListener.ldapProperties);
            domainStr = prop.getProperty("domainStr", "dc=maxcrc,dc=com");
            url = prop.getProperty("url", "ldap://localhost:389");
            orgUnitStr = prop.getProperty("orgUnitStr", "People");
            defaultOrgUnitStr = orgUnitStr;
            mail = prop.getProperty("mail", "@maxcrc.com");
            adminName = prop.getProperty("adminName", "cn=Manager,dc=maxcrc,dc=com");
            adminPwd = prop.getProperty("adminPwd", "secret");
            //
        } catch(IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
        }
    }
    
    public Hashtable<Object, Object> getEnv() {
        final Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxt);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, conntype);
        return env;
    }
    
    public DirContext getAdminContext(Hashtable<Object, Object> env) {
        DirContext dctx = null;
        try {
            env.put(Context.SECURITY_PRINCIPAL, adminName);
            env.put(Context.SECURITY_CREDENTIALS, adminPwd);
            return new InitialDirContext(env);
        } catch(NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return dctx;
        }
    }
    
    public void addUser() {
        addUser(false);
    }

    public void addUser(Boolean force) {
        // Create a container set of attributes
        final Attributes container = new BasicAttributes(true);

        // Create the objectclass to add
        final Attribute objClasses = new BasicAttribute("objectClass");
        objClasses.add("top");
        objClasses.add("person");
        objClasses.add("organizationalperson");
        objClasses.add("user");
        container.put(objClasses);

        // Assign the username, first name, and last name
        final String fullName = new StringBuffer().append(userFirstName).append(" ").append(userLastName).toString();
        container.put(new BasicAttribute("cn", fullName));
        container.put(new BasicAttribute("sAMAccountName", userId));
        container.put(new BasicAttribute("userPrincipalName", new StringBuffer().append(userId).append(mail).toString()));
        container.put(new BasicAttribute("givenName", userFirstName));
        container.put(new BasicAttribute("sn", userLastName));
        container.put(new BasicAttribute("displayName", fullName));

        // Create the entry
        try {
            final DirContext dctx = getAdminContext(getEnv());
            //Verify if should remove user first
            if (force) {
                try {
                    final DirContext o = (DirContext) dctx.lookup(getUserDN());
                    o.getAttributes("");
                    dctx.unbind(getUserDN());
                } catch (NamingException ex) {
                    log.info(ex);
                    //ignore
                }
            }
            dctx.createSubcontext(getUserDN(), container);
            dctx.close();
            if (null != dctx) {
                updateUserPassword(false);
            }
        } catch (NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
        }
    }
    
    public void updateUserPassword(Boolean change) {
	try {
            String newQuotedPassword = "\"" + userPass + "\"";
            byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");
            ModificationItem[] mods = new ModificationItem[2];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", newUnicodePassword));
            String userAccountControl;
            if (change) {
                userAccountControl = Integer.toString(NOT_CHANGE_PASS_LOGON);
            } else {
                userAccountControl = Integer.toString(UF_NORMAL_ACCOUNT + UF_PASSWORD_EXPIRED);
            }
            mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userAccountControl", userAccountControl));
            userModification(mods);
	}
	catch (UnsupportedEncodingException ex) {
	    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
	}
    }
    
    public void contextModification(ModificationItem[] mods) {
	try {
            final DirContext dctx = getAdminContext(getEnv());
	    dctx.modifyAttributes(domainStr, mods);
            dctx.close();
	}
	catch (NamingException ex) {
	    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
	}        
    }
    
    public void userModification(ModificationItem[] mods) {
	try {
            final DirContext dctx = getAdminContext(getEnv());
	    dctx.modifyAttributes(searchUserDN(), mods);
            dctx.close();
	}
	catch (NamingException ex) {
	    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
	}        
    }
      
    public Attributes searchUser(String[] srchResult) {
        Attributes attrs = null;
        // Create a search control
        final SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        constraints.setReturningAttributes(srchResult);

        final Attributes container = new BasicAttributes();
        final Attribute atrUid = new BasicAttribute("cn", userId);
        final Attribute AtrOu = new BasicAttribute("ou", orgUnitStr);
        container.put(atrUid);
        container.put(AtrOu);

        try {
            final DirContext dctx = getAdminContext(getEnv());
            final NamingEnumeration ne = dctx.search(searchUserDN(), "(objectClass=*)", constraints);
            dctx.close();

            if(ne.hasMore()) {
                final SearchResult rs= (SearchResult)ne.next();
                attrs = rs.getAttributes();
                return attrs;
            }
            return attrs;
        } catch (NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return attrs;
        }
    }
    
    public NamingEnumeration searchDomain() {
        return searchDomain(null, null, null);
    }
    
    public NamingEnumeration searchDomain(String[] fields) {
        return searchDomain(fields, null, null);
    }
    
    public NamingEnumeration searchDomain(String[] fields, String filter) {
        return searchDomain(fields, filter, null);
    }
     
    public NamingEnumeration searchDomain(String[] fields, String filter, String scope) {
        NamingEnumeration values = null;
        //Create a search control
        final SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        //Determine which fields should be returned
        if (fields != null) {
            constraints.setReturningAttributes(fields);
        }
        //Determine the Filters os the Search
        String srchFilter =  "(objectClass=*)"; 
        if (filter != null && !filter.isEmpty()) {
          srchFilter = filter;
        }
        //Determine the search scope
        String srchScope = domainStr;
        if (scope != null && !scope.isEmpty()) {
          srchScope = scope;
        }
        //Begin Search
        try {
            final DirContext dctx = getAdminContext(getEnv());
            values = dctx.search(srchScope, srchFilter, constraints);
            dctx.close();
            return values;
        } catch (NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return values;
        }
    }
        
    public Attributes singleSearch(NamingEnumeration values) {
        Attributes attrs = null;
        try {
            while (values.hasMoreElements()) {
                SearchResult result = (SearchResult) values.next();
                attrs = result.getAttributes();
                return attrs;
            }
        } catch (NamingException|NullPointerException ex) {
            Logger.getLogger(LDAPUtils.class.getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return attrs;
        }
        return attrs;
    }

    public Boolean authUser() {
        Hashtable<Object, Object> env = getEnv();
        env.put(Context.SECURITY_PRINCIPAL, searchUserDN());
        env.put(Context.SECURITY_CREDENTIALS, userPass);

        try {
            DirContext dctx = new InitialDirContext(env);
            dctx.close();
            // user is authenticated
            return true;	
        } catch (NamingException ex) {	
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return false;
        }
    }
    
    public String searchUserDN() {
        String result = new String();
        final String[] fields = {"distinguishedName"};
        final String filter = new StringBuffer().append("(|(uid=").append(userId).append("),(sAMAccountName=").append(userId).append("))").toString();
        try {
            final NamingEnumeration search = searchDomain(fields, filter);
            try {
                result = singleSearch(search).get("distinguishedName").get(0).toString();
            } catch (NullPointerException ex) {
                Logger.getLogger(LDAPUtils.class.getName()).log(Level.SEVERE, null, ex);
                log.error(ex);
                return result;
            }
        } catch (NamingException|ArrayIndexOutOfBoundsException ex) {
            Logger.getLogger(LDAPUtils.class.getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return result;
        }
        return result;
    }
    
    public String getUserDN() {
        final String userCN = new StringBuffer().append("cn=").append(userFirstName).append(" ").append(userLastName).toString();
        return new StringBuffer().append(userCN).append(",").append(getOuDN()).toString();
    }
    
    public String getOuDN() {
        final String orgUnit = new StringBuffer().append("ou=").append(orgUnitStr).toString();
        final String domain = new StringBuffer().append(",").append(domainStr).toString();
        return new StringBuffer().append(orgUnit).append(domain).toString();
    }

    public Attributes getAttrs() {
        Attributes attrs = null;
        // Set up the environment for creating the initial context
        Hashtable<Object, Object> env = getEnv();
        try {
            // Create the initial context
            final DirContext dctx = new InitialDirContext(env);

            // Get all the attributes of named object
            attrs = dctx.getAttributes(searchUserDN());
            dctx.close();
            return attrs;
        } catch (Exception ex) {
            Logger.getLogger(LDAPUtils.class.getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
            return attrs;
        }
    }
    
    public void printAttrs(NamingEnumeration nam) {
        try {
            while (nam.hasMoreElements()) {
                SearchResult result = (SearchResult) nam.next();
                Attributes attribs = result.getAttributes();
                if (null != attribs) {
                    for (NamingEnumeration ae = attribs.getAll(); ae.hasMoreElements();) {
                        Attribute atr = (Attribute) ae.next();
                        String attributeID = atr.getID();
                        for (Enumeration vals = atr.getAll(); 
                            vals.hasMoreElements(); 
                            System.out.println(attributeID +": "+ vals.nextElement()));
                    }
                }              
            }
        } catch (NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
        }
    }
           
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url.toLowerCase();
    }

    public String getDomainStr() {
        return domainStr;
    }

    public void setDomainStr(String domainStr) {
        this.domainStr = domainStr.toLowerCase();
    }

    public String getOrgUnitStr() {
        return orgUnitStr;
    }

    public void setOrgUnitStr(String orgUnitStr) {
        this.orgUnitStr = orgUnitStr.toLowerCase();
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail.toLowerCase();
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName.toLowerCase();
    }

    public String getAdminPwd() {
        return adminPwd;
    }

    public void setAdminPwd(String adminPwd) {
        this.adminPwd = adminPwd;
    }
    
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId.toLowerCase();
    }

    public String getUserPass() {
        return userPass;
    }

    public void setUserPass(String userPass) {
        this.userPass = userPass;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }
    
    public void setDefaultOrgUnitStr() {
        this.orgUnitStr = this.defaultOrgUnitStr;
    }

}