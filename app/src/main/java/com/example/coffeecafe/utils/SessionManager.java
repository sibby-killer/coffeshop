package com.example.coffeecafe.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static SessionManager instance;
    private final SharedPreferences prefs;

    private static final String PREFS_NAME = "CoffeeShopSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveLoginSession(String userId, String role, String name, String email) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public boolean isAdmin() {
        return "admin".equals(getUserRole());
    }

    public boolean isShopOwner() {
        return "shop_owner".equals(getUserRole());
    }

    public boolean isCustomer() {
        return "customer".equals(getUserRole());
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
