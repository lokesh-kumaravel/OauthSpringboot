package com.lokesh.OauthSpringboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokesh.OauthSpringboot.Services.AuthResponse;
import com.lokesh.OauthSpringboot.Services.AuthService;
import com.lokesh.OauthSpringboot.Services.GoogleOAuthTokenResponse;
import com.lokesh.OauthSpringboot.service.AuthService2;
import com.lokesh.OauthSpringboot.service.JWTService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class Controller {

    @Value("${chronify.google.oauth.redirect_uri}")
    private String redirectUri;

    @Value("${chronify.google.oauth.auth_uri}")
    private String authUri;

    @Value("${chronify.google.oauth.client_id}")
    private String clientId;

    @Value("${chronify.google.oauth.client_secret}")
    private String clientSecret; // Not used in the current example, but included for completeness.

    @GetMapping("/")
    public String home() {
        return "Hello World";
    }

    @CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = { RequestMethod.GET,
            RequestMethod.OPTIONS })
    @GetMapping("/login")
    public Map<String, String> login() {
        try {
            StringBuilder queryString = new StringBuilder();
            queryString.append("redirect_uri=")
                    .append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()))
                    .append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString()))
                    .append("&scope=")
                    .append(URLEncoder.encode("email profile openid", StandardCharsets.UTF_8.toString())) // Example
                                                                                                          // scopes
                    .append("&access_type=offline")
                    .append("&response_type=code")
                    .append("&prompt=consent");

            URI uri = URI.create(authUri + "?" + queryString.toString());

            // Return a JSON response with the URL
            Map<String, String> response = new HashMap<>();
            response.put("url", uri.toString());

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap("error", "Error generating URL");
        }
    }

    @Autowired
    private AuthService authService;

    @Autowired 
    private AuthService2 authService2;

    @Autowired
    private UserRepo userrepo;

    @PostMapping("/oauth/google")
    public ResponseEntity<Map<String, Object>> handleGoogleCallback(@RequestParam String code) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("code : " + code);
        try {
            // Use the service to get tokens
            GoogleOAuthTokenResponse tokenResponse = authService.getGoogleOAuthTokens(code);

            if (tokenResponse != null) {
                // Extract user info from token response
                String userInfoJson = authService.getGoogleUserInfo(tokenResponse.getIdToken(), tokenResponse.getAccessToken()).get();
                
                // Parse the userInfo JSON into an object
                Map<String, Object> userInfo = new ObjectMapper().readValue(userInfoJson, HashMap.class);
                
                String email = (String) userInfo.get("email");
                String name = (String) userInfo.get("name");
                String picture = (String) userInfo.get("picture");

                // Check if the user already exists in the database
                User existingUser = userrepo.findByEmail(email);
                if (existingUser == null) {
                    // If user does not exist, create a new user
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUsername(name);
                    newUser.setPicture(picture);

                    // Save user to MongoDB
                    userrepo.save(newUser);
                } else {
                    // If user exists, update their details (optional)
                    existingUser.setUsername(name);
                    existingUser.setPicture(picture);
                    userrepo.save(existingUser);
                }
                AuthResponse authResponse = authService2.verify(existingUser);
                System.out.println("JWT TOKEN : "+authResponse.getToken());
                response.put("userInfo", userInfo); // Add user info to response
                response.put("message", "Login successful");
                response.put("authResponse", authResponse);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            System.out.println("Error In Getting id_tokens: " + e.getMessage());
            response.put("error", "Failed to login with Google");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        response.put("error", "Invalid Google login attempt");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/profile")
    public String getMethodName(@RequestParam String param) {
        return "This is the profile page";
    }
    

    @Autowired
    private JWTService jwtservice;
    @Autowired
    private MyUserDetailsService myUserDetailsService;
    @PostMapping("/jwtcheck")
public ResponseEntity<?> jwtCheck(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7); // Extract token part
        String username = jwtservice.extractUserName(token);
        System.out.println(username+";;;;;;;;;;;;");
        if (username != null) {
            UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);
            
            if (jwtservice.validateToken(token, userDetails)) {
                System.out.println("SUCCESSFULLY VALIDATED");
                return ResponseEntity.ok(Collections.singletonMap("message", "JWT token is valid.")); // Return a JSON object
            }
        }

    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
}
}

// http://localhost:8082/oauth/google?
// code=4%2F0AVG7fiRNRhqL3W9wFe6v4k66fVESNNdeU3HE5rZD-GkoV1qD0ygChuGbDAG5Sk1wpU-GdQ&
// scope=email+profile+openid+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email&
// authuser=0&
// prompt=consent
