package com.dinamica.services;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "JsonParserServlet", urlPatterns = {"/JsonParserServlet"})
public class JsonParserServlet extends HttpServlet {
    
    final LDAPUtils ldap = new LDAPUtils();
 
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JsonObject json = new JsonObject();
        try {
            String username = new String();
            String password = new String();
            
            String action = new String();
            action = request.getParameter("action");
                
            if (handleAuthHeader(request.getHeader("Authorization"), username, password)) {
     
                ActionsHandler actionHdlr = new ActionsHandler();
                
                if (action != null && !action.isEmpty()) {
                    json = actionHdlr.run(action.toLowerCase(), request);
                } else {
                    json.addProperty("result", "error");
                    json.addProperty("message", "No action set");
                }
            } else {
                json.addProperty("result", "error");
                json.addProperty("action", action);
                json.addProperty("message", "Athentication failed");
            }
        } catch(Exception ex) {
            Logger.getLogger(LDAPUtils.class.getName()).log(Level.SEVERE, null, ex);
            json.addProperty("result", "exception");
            json.addProperty("message", ex.toString());
            json.addProperty("stacktrace", Utils.getStackTrace(ex));
        }
        response.setContentType("application/json");
        response.getOutputStream().print(json.toString());
        response.getOutputStream().flush();
    }
    
    @Override
      protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
              throws ServletException, IOException {
          setAccessControlHeaders(resp);
          resp.setStatus(HttpServletResponse.SC_OK);
      }
 
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setAccessControlHeaders(response);
        processRequest(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    @Override
    public String getServletInfo() {
        return "Short description";
    }
    
    private void setAccessControlHeaders(HttpServletResponse resp) {
      resp.setHeader("Access-Control-Allow-Origin", "*");
      resp.setHeader("Access-Control-Allow-Method", "GET");
      resp.setHeader("Access-Control-Allow-Headers", "access-control-allow-origin,authorization");
    }
    
    private Boolean handleAuthHeader(String auth, String username, String password) {
        if (auth != null && auth.startsWith("Basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = auth.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials),
                    Charset.forName("UTF-8"));
            // credentials = username:password
            final int size = credentials.length();
            if (size >= 2) {
                final String[] values = credentials.split(":",size);
                // Validate Active Directory Account
                username = values[0];
                password = values[1];
                ldap.setUserId(username);
                ldap.setUserPass(password);
                return ldap.authUser();
            }
        }
        return false;
    }
}
