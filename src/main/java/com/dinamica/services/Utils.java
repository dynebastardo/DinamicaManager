package com.dinamica.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

public class Utils {
    
    final private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ActionsHandler.class);
    
    public static String hashAndEncodePassword(String password) {
        try {
            final byte[] md5 = DigestUtils.md5(password.trim().getBytes("UTF-8"));
            final byte[] base64 = Base64.encodeBase64(md5);
            final String hashedAndEncoded = new String(base64, "ASCII");
            return "{MD5}" + hashedAndEncoded;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return password;
    }
    
    public JsonObject attrsToJSON(Attributes attrs) {
        JsonObject json = new JsonObject();
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
        return json;
    }
    
    public static JsonObject nEnumToJSON(NamingEnumeration nam) {
        JsonObject jsonUp = new JsonObject();
        JsonObject json = new JsonObject();
        int i = 1;
        while (nam.hasMoreElements()) {
            try {
                SearchResult result = (SearchResult) nam.next();
                Attributes attribs = result.getAttributes();
                if (null != attribs) {
                    if (!json.entrySet().isEmpty()) {
                        jsonUp.add(Integer.toString(i), json);
                        json = new JsonObject();
                        i++;
                    }
                    for (NamingEnumeration ae = attribs.getAll(); ae.hasMoreElements();) {
                        Attribute atr = (Attribute) ae.next();
                        String attributeID = atr.getID();
                        for (Enumeration vals = atr.getAll(); vals.hasMoreElements();) {
                            Object value = vals.nextElement();
                            try {
                                json.addProperty(new String(attributeID.getBytes("WINDOWS-1250"),"ISO-8859-1"), new String(value.toString().getBytes("WINDOWS-1250"),"ISO-8859-1"));
                            } catch (UnsupportedEncodingException ex) {
                                Logger.getLogger(JsonParserServlet.class.getName()).log(Level.SEVERE, null, ex);
                                log.error(ex);
                            }
                        } 
                    }
                }
                if (!json.entrySet().isEmpty()) {
                    jsonUp.add(Integer.toString(i), json);
                }
            } catch (NamingException ex) { 
                Logger.getLogger(JsonParserServlet.class.getName()).log(Level.SEVERE, null, ex);
                log.error(ex);
            }
        }
        return jsonUp;
    }    
    
    public static String getStackTrace(Throwable ex) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        ex.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}
