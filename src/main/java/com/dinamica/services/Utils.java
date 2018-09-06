package com.dinamica.services;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

public class Utils {
    
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
    
    public JSONObject attrsToJSON(Attributes attrs) {
        JSONObject json = new JSONObject();
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
    
}
