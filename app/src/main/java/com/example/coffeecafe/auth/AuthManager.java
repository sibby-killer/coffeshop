package com.example.coffeecafe.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.coffeecafe.BuildConfig;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Profile;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private static AuthManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private Profile currentProfile;

    private static final String PREFS_NAME = "CoffeeShopAuth";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PROFILE = "user_profile";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private final Context appContext;

    private AuthManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    public interface AuthCallback {
        void onSuccess(Profile profile);
        void onError(String error);
        default void onEmailNotConfirmed(String email) {
            onError("Please check your email and verify your account before logging in.");
        }
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public void signUp(String email, String password, String fullName, String phone, String role, AuthCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("full_name", fullName);
                metadata.put("phone", phone);
                metadata.put("role", role);

                Map<String, Object> body = new HashMap<>();
                body.put("email", email);
                body.put("password", password);
                body.put("data", metadata);

                String jsonBody = gson.toJson(body);
                String response = SupabaseApi.getInstance().postAuth("signup", jsonBody);

                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    String userId = user.has("id") ? user.get("id").getAsString() : "";

                    // Store access token if present
                    if (jsonResponse.has("access_token")) {
                        saveAccessToken(jsonResponse.get("access_token").getAsString());
                    }

                    Profile profile = new Profile(userId, fullName, phone, role);
                    saveSession(userId, email, profile);
                    currentProfile = profile;
                    callback.onSuccess(profile);
                } else if (jsonResponse.has("error")) {
                    String errorMsg = jsonResponse.get("error").getAsString();
                    callback.onError(errorMsg);
                } else {
                    callback.onError("Signup failed - unexpected response");
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                if (msg.contains("AUTH_ERROR")) {
                    String[] parts = msg.split("\\|", 3);
                    if (parts.length >= 3) {
                        String errorBody = parts[2];
                        try {
                            JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                            String errorDesc = errorJson.has("error_description")
                                    ? errorJson.get("error_description").getAsString()
                                    : errorJson.has("msg")
                                    ? errorJson.get("msg").getAsString()
                                    : errorJson.has("error")
                                    ? errorJson.get("error").getAsString()
                                    : errorBody;
                            callback.onError(errorDesc);
                        } catch (Exception parseEx) {
                            callback.onError(errorBody);
                        }
                    } else {
                        callback.onError(msg);
                    }
                } else {
                    callback.onError(msg);
                }
            }
        }).start();
    }

    public void signIn(String email, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("email", email);
                body.put("password", password);

                String jsonBody = gson.toJson(body);
                String response = SupabaseApi.getInstance().postAuth("token?grant_type=password", jsonBody);

                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

                if (jsonResponse.has("user")) {
                    JsonObject user = jsonResponse.getAsJsonObject("user");
                    String userId = user.has("id") ? user.get("id").getAsString() : "";
                    String userEmail = user.has("email") ? user.get("email").getAsString() : email;

                    // Store access token
                    if (jsonResponse.has("access_token")) {
                        saveAccessToken(jsonResponse.get("access_token").getAsString());
                    }

                    Profile profile = fetchProfile(userId);
                    if (profile != null) {
                        saveSession(userId, userEmail, profile);
                        currentProfile = profile;
                        callback.onSuccess(profile);
                    } else {
                        // Profile not found - create one from user data
                        String fullName = "";
                        String phone = "";
                        String role = "customer";
                        if (user.has("user_metadata")) {
                            JsonObject metadata = user.getAsJsonObject("user_metadata");
                            if (metadata.has("full_name")) fullName = metadata.get("full_name").getAsString();
                            if (metadata.has("phone")) phone = metadata.get("phone").getAsString();
                            if (metadata.has("role")) role = metadata.get("role").getAsString();
                        }
                        profile = new Profile(userId, fullName, phone, role);
                        saveSession(userId, userEmail, profile);
                        currentProfile = profile;
                        callback.onSuccess(profile);
                    }
                } else if (jsonResponse.has("error")) {
                    String errorMsg = jsonResponse.get("error").getAsString();
                    callback.onError(errorMsg);
                } else {
                    callback.onError("Login failed - unexpected response");
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                if (msg.contains("AUTH_ERROR")) {
                    String[] parts = msg.split("\\|", 3);
                    if (parts.length >= 3) {
                        String errorBody = parts[2];
                        try {
                            JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                            // Check for email_not_confirmed code (Supabase standard)
                            String errorCode = errorJson.has("code") ? errorJson.get("code").getAsString() : "";
                            String errorDesc = errorJson.has("error_description")
                                    ? errorJson.get("error_description").getAsString()
                                    : errorJson.has("msg")
                                    ? errorJson.get("msg").getAsString()
                                    : errorJson.has("error")
                                    ? errorJson.get("error").getAsString()
                                    : errorBody;
                            if (errorCode.equals("email_not_confirmed")
                                    || errorDesc.toLowerCase().contains("email not confirmed")
                                    || errorDesc.toLowerCase().contains("email_not_confirmed")) {
                                callback.onEmailNotConfirmed(email);
                            } else {
                                callback.onError(errorDesc);
                            }
                        } catch (Exception parseEx) {
                            callback.onError(errorBody);
                        }
                    } else {
                        callback.onError(msg);
                    }
                } else {
                    callback.onError(msg);
                }
            }
        }).start();
    }

    public void signOut(SimpleCallback callback) {
        new Thread(() -> {
            try {
                clearSession();
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private Profile fetchProfile(String userId) {
        try {
            String token = getAccessToken();
            String query = "id=eq." + userId + "&select=*";
            String response = SupabaseApi.getInstance().get("profiles", query, token);

            // Supabase returns an array
            if (response.startsWith("[")) {
                Profile[] profiles = gson.fromJson(response, Profile[].class);
                if (profiles != null && profiles.length > 0) {
                    return profiles[0];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void saveSession(String userId, String email, Profile profile) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PROFILE, gson.toJson(profile))
                .apply();

        // Also sync to SessionManager so all fragments can access the userId
        SessionManager.getInstance(appContext).saveLoginSession(
                userId,
                profile.getRole(),
                profile.getFullName(),
                email
        );
    }

    private void saveAccessToken(String token) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    private void clearSession() {
        prefs.edit().clear().apply();
        SessionManager.getInstance(appContext).clearSession();
        currentProfile = null;
    }

    public boolean isLoggedIn() {
        return prefs.contains(KEY_USER_ID);
    }

    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getCurrentEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public Profile getCurrentProfile() {
        if (currentProfile == null) {
            String profileJson = prefs.getString(KEY_PROFILE, null);
            if (profileJson != null) {
                currentProfile = gson.fromJson(profileJson, Profile.class);
            }
        }
        return currentProfile;
    }

    public String getCurrentRole() {
        Profile profile = getCurrentProfile();
        return profile != null ? profile.getRole() : null;
    }

    /**
     * Send a password reset email via Supabase.
     */
    public void resetPassword(String email, SimpleCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("email", email);

                String jsonBody = gson.toJson(body);
                SupabaseApi.getInstance().postAuth("recover", jsonBody);

                callback.onSuccess();
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to send reset email";
                if (msg.contains("AUTH_ERROR")) {
                    String[] parts = msg.split("\\|", 3);
                    if (parts.length >= 3) {
                        String errorBody = parts[2];
                        try {
                            JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                            String errorDesc = errorJson.has("error_description")
                                    ? errorJson.get("error_description").getAsString()
                                    : errorJson.has("msg")
                                    ? errorJson.get("msg").getAsString()
                                    : errorBody;
                            callback.onError(errorDesc);
                        } catch (Exception parseEx) {
                            callback.onError(errorBody);
                        }
                    } else {
                        callback.onError(msg);
                    }
                } else {
                    callback.onError(msg);
                }
            }
        }).start();
    }

    /**
     * Save session locally without Supabase (for hardcoded admin bypass).
     */
    public void saveLocalSession(Profile profile) {
        saveSession(profile.getId(), BuildConfig.ADMIN_EMAIL, profile);
    }
}
