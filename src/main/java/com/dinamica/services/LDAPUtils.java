package com.dinamica.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.AuthenticationException;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

class LDAPUtils {
    
    final String conntype  = "simple";
    final String ldapCtxt  = "com.sun.jndi.ldap.LdapCtxFactory";
    final Hashtable<Object, Object> env = new Hashtable<Object, Object>();
    
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
    private DirContext dctx;
    
    public LDAPUtils() {
        final Properties prop = new Properties();        
        try {
            final InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
            prop.load(input);
            domainStr = prop.getProperty("domainStr", "dc=maxcrc,dc=com");
            url = prop.getProperty("url", "ldap://localhost:389");
            orgUnitStr = prop.getProperty("orgUnitStr", "People");
            defaultOrgUnitStr = orgUnitStr;
            mail = prop.getProperty("mail", "@maxcrc.com");
            adminName = prop.getProperty("adminName", "cn=Manager,dc=maxcrc,dc=com");
            adminPwd = prop.getProperty("adminPwd", "secret");
            //
            env.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxt);
            env.put(Context.PROVIDER_URL, url);
            env.put(Context.SECURITY_AUTHENTICATION, conntype);
            env.put(Context.SECURITY_PRINCIPAL, adminName);
            env.put(Context.SECURITY_CREDENTIALS, adminPwd);
            dctx = new InitialDirContext(env);
            //
        } catch(IOException|NamingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addUser() throws NamingException {
        // Create a container set of attributes
        final Attributes container = new BasicAttributes();

        // Create the objectclass to add
        final Attribute objClasses = new BasicAttribute("objectClass");
        objClasses.add("inetOrgPerson");

        // Assign the username, first name, and last name
        final Attribute commonName = new BasicAttribute("cn", new StringBuffer().append(userFirstName).append(" ").append(userLastName).toString());
        final Attribute email = new BasicAttribute("mail", new StringBuffer().append(userId).append(mail).toString());
        final Attribute givenName = new BasicAttribute("givenName", userFirstName);
        final Attribute uid = new BasicAttribute("uid", userId);
        final Attribute surName = new BasicAttribute("sn", userLastName);

        // Add password
        final Attribute userPassword = new BasicAttribute("userpassword", Utils.hashAndEncodePassword(userPass));

        // Add these to the container
        container.put(objClasses);
        container.put(commonName);
        container.put(givenName);
        container.put(email);
        container.put(uid);
        container.put(surName);
        container.put(userPassword);

        // Create the entry
        dctx.createSubcontext(getUserDN(), container);
        
        if (null != dctx) {
            try {
                dctx.close();
            } catch (final NamingException e) {
                System.out.println("Error in closing ldap " + e);
            }
        }
    }
    
    public Attributes searchUser(String[] srchResult) throws NamingException {
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
            final NamingEnumeration ne = dctx.search(getUserDN(), "(objectClass=*)", constraints);

            if(ne.hasMore()) {
                final SearchResult rs= (SearchResult)ne.next();
                final Attributes attrs = rs.getAttributes();
                return attrs;
            }
            return null;
        } catch (NameNotFoundException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public NamingEnumeration searchDom(String[] srchResult) throws NamingException {
        // Create a search control
        final SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if (srchResult.length > 0) {
            constraints.setReturningAttributes(srchResult);
        }
        //Begin Search
        try {
            final NamingEnumeration values = dctx.search(domainStr, "(objectClass=*)", constraints);
            while (values.hasMoreElements()) {
                SearchResult result = (SearchResult) values.next();
                Attributes attribs = result.getAttributes();
                if (null != attribs) {
                    if (null == attribs.get("ou")) {
                        for (NamingEnumeration ae = attribs.getAll(); ae.hasMoreElements();) {
                            Attribute atr = (Attribute) ae.next();
                            String attributeID = atr.getID();
                            for (Enumeration vals = atr.getAll(); 
                                vals.hasMoreElements(); 
                                System.out.println(attributeID +": "+ vals.nextElement()));
                        }
                    }
                }              
            }
            return null;
        } catch (NameNotFoundException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public NamingEnumeration searchOu(String[] srchResult) throws NamingException {
        // Create a search control
        final SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        constraints.setReturningAttributes(srchResult);
        //Define Scope as OrganizationUnit
        final Attributes container = new BasicAttributes();
        final Attribute AtrOu = new BasicAttribute("ou", orgUnitStr);
        container.put(AtrOu);
        //Begin Search
        try {
            final NamingEnumeration ne = dctx.search(getUserDN(), "(objectClass=*)", constraints);
            return ne;
        } catch (NameNotFoundException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public Boolean authUser() {
        Hashtable<String, String> environment = 
                new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxt);
        environment.put(Context.PROVIDER_URL, url);
        environment.put(Context.SECURITY_AUTHENTICATION, conntype);
        environment.put(Context.SECURITY_PRINCIPAL, getUserDN());
        environment.put(Context.SECURITY_CREDENTIALS, userPass);

        try {
            final DirContext authContext = new InitialDirContext(environment);
            // user is authenticated
            return true;	
        } catch (AuthenticationException ex) {	
            // Authentication failed
            return false;

        } catch (NamingException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public String getUserDN() {
        final String userCN = new StringBuffer().append("cn=").append(userId).toString();
        final String orgUnit = new StringBuffer().append(",ou=").append(orgUnitStr).toString();
        final String domain = new StringBuffer().append(",").append(domainStr).toString();
        return new StringBuffer().append(userCN).append(orgUnit).append(domain).toString();
    }

    public Attributes getAttrs() {
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxt);
        env.put(Context.PROVIDER_URL, url);
        try {
            // Create the initial context
            final DirContext ctx = new InitialDirContext(env);

            // Get all the attributes of named object
            return ctx.getAttributes(getUserDN());
        } catch (Exception ex) {
          ex.printStackTrace();
          return null;
        }
    }
    
    public void printAttrs() {
        final Attributes attrs = getAttrs();

        // Print the answer
        if (attrs == null) {
          System.out.println("No attributes");
        } else {
          /* Print each attribute */
          try {
            for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
              final Attribute attr = (Attribute) ae.next();
              System.out.println("attribute: " + attr.getID());

              /* print each value */
              for (NamingEnumeration e = attr.getAll(); e.hasMore(); e.toString()) {
                  if (attr.getID().equals("userPassword")) {
                    final String pwd = new String((byte[]) e.next());
                    System.out.println(pwd);
                  } else {
                    System.out.println("value: " + e.next());
                  }
              }
            }
          } catch (NamingException e) {
            e.printStackTrace();
          }
        }
    }
        
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDomainStr() {
        return domainStr;
    }

    public void setDomainStr(String domainStr) {
        this.domainStr = domainStr;
    }

    public String getOrgUnitStr() {
        return orgUnitStr;
    }

    public void setOrgUnitStr(String orgUnitStr) {
        this.orgUnitStr = orgUnitStr;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
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
        this.userId = userId;
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
