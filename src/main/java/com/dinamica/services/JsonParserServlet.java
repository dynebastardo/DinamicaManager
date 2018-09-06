package com.dinamica.services;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONObject;

@WebServlet(name = "JsonParserServlet", urlPatterns = {"/JsonParserServlet"})
public class JsonParserServlet extends HttpServlet {
    
    final LDAPUtils ldap = new LDAPUtils();
 
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            if (handleAuthHeader(request.getHeader("Authorization"))) {
     
                Gson gson = new Gson();
                ActionsHandler actionHdlr = new ActionsHandler(ldap);
                JSONObject json = new JSONObject();
                String action = new String();
                
                action = request.getParameter("action");
                if (action != null && !action.isEmpty()) {
                    json = actionHdlr.run(action, request);
                    response.setContentType("application/json");
                    response.getOutputStream().print(gson.toJson(json));    
                } else {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getOutputStream().print("No action set");
                }
            }
        } catch(Exception ex) {
            response.getOutputStream().print(ExceptionUtils.getStackTrace(ex));
        }
        response.getOutputStream().flush();
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
      resp.setHeader("Access-Control-Allow-Methods", "GET");
    }
    
    private Boolean handleAuthHeader(String auth) {
        if (auth != null && auth.startsWith("Basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = auth.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials),
                    Charset.forName("UTF-8"));
            // credentials = username:password
            final String[] values = credentials.split(":",2);
            // Validate Active Directory Account
            ldap.setUserId(values[0]);
            ldap.setUserPass(values[1]);
            ldap.setDefaultOrgUnitStr();
            return ldap.authUser();
        }
        return false;
    }
}
