package com.lokesh.ecom_proj.task;

public class configurer {
    
    // Replace with your actual secret key
    private static final String GOOGLE_CLIENT_SECRET = "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";
    private static final String GOOGLE_CLIENT_KEY = "538763792750-6tl59ihs2buelr9heogc8hpldtsj4ifp.apps.googleusercontent.com";
    public static String getGoogleClientSecret() {
        return GOOGLE_CLIENT_SECRET;
    }
    public static String getGoogleClientKey() {
        return GOOGLE_CLIENT_KEY;
    }
}
