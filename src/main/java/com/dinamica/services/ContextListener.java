package com.dinamica.services;
 
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
 
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
 
import org.apache.log4j.PropertyConfigurator;
 
@WebListener("application context listener")

public class ContextListener implements ServletContextListener {
    
    final private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ContextListener.class);
    private static ServletContext context;
    public static InputStream ldapProperties;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // initialize log4j here
        context = event.getServletContext();
        String configFile = context.getInitParameter("log4j-config-location");
        String fullPath = context.getRealPath("") + File.separator + configFile;
        PropertyConfigurator.configure(fullPath);
        
        //Initialize ldap here
//        configFile = context.getInitParameter("ldap-config-location");
//        ldapProperties = context.getResourceAsStream(configFile);
//        if (ldapProperties == null) {
//            log.error("ldap.properties NOT loaded");
//        } else {
//            log.info("ldap.properties file loaded");
//        }
    }
     
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try {
            ldapProperties.close();
        } catch (NullPointerException|IOException ex) {
            log.error(ex);
        }
    }  
    
    public static InputStream loadLdapProps() {
        final String configFile = context.getInitParameter("ldap-config-location");
        ldapProperties = context.getResourceAsStream(configFile);
        if (ldapProperties == null) {
            log.error("ldap.properties NOT loaded");
        } else {
            log.info("ldap.properties file loaded");
        }
        return ldapProperties;
    }
    
    public static void closeLdapProps() throws IOException {
        try {
            ldapProperties.close();
        } catch (NullPointerException|IOException ex) {
            Logger.getLogger(context.getClass().getName()).log(Level.SEVERE, null, ex);
            log.error(ex);
        }
    }
}
