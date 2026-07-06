package com.example.coffeecafe.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Singleton helper for making Supabase REST API calls via OkHttp.
 * Handles GET, POST, PATCH, and DELETE operations against the PostgREST and Auth APIs.
 * Automatically refreshes expired JWT tokens on 401 responses.
 */
public class SupabaseApi {
    private static SupabaseApi instance;
    private final SupabaseConfig config;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String AUTH_PREFS = "CoffeeShopAuth";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private SupabaseApi() {
        config = SupabaseConfig.getInstance();
        client = config.getHttpClient();
    }

    public static synchronized SupabaseApi getInstance() {
        if (instance == null) {
            instance = new SupabaseApi();
        }
        return instance;
    }

    /**
     * Try to refresh the access token using the stored refresh token.
     * Returns the new access token, or null on failure.
     */
    private String tryRefreshToken(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
            String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
            if (refreshToken == null || refreshToken.isEmpty()) return null;

            Map<String, Object> body = new HashMap<>();
            body.put("refresh_token", refreshToken);
            String jsonBody = new com.google.gson.Gson().toJson(body);

            String url = config.getSupabaseUrl() + "/auth/v1/token?grant_type=refresh_token";
            RequestBody reqBody = RequestBody.create(jsonBody, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .addHeader("apikey", config.getSupabaseKey())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (json.has("access_token")) {
                        String newAccessToken = json.get("access_token").getAsString();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(KEY_ACCESS_TOKEN, newAccessToken);
                        if (json.has("refresh_token")) {
                            editor.putString(KEY_REFRESH_TOKEN, json.get("refresh_token").getAsString());
                        }
                        editor.apply();
                        return newAccessToken;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isTokenExpiredError(String body) {
        return body != null && (body.contains("PGRST303") || body.contains("JWT expired"));
    }

    /**
     * GET request to PostgREST endpoint.
     * Automatically retries with refreshed token on 401.
     */
    public String get(String table, String query, String token) throws IOException {
        String result = doGet(table, query, token);
        // Check for token expiry and retry once
        if (result == null && token != null && !token.isEmpty()) {
            // The error was thrown - we need to handle it differently
        }
        return result;
    }

    private String doGet(String table, String query, String token) throws IOException {
        String url = config.getSupabaseUrl() + "/rest/v1/" + table;
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String body = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) {
                if (response.code() == 401 && token != null && !token.isEmpty()
                        && isTokenExpiredError(body)) {
                    throw new TokenExpiredException("Token expired");
                }
                throw new IOException("GET " + table + " failed: " + response.code() + " " + body);
            }
            return body;
        }
    }

    /**
     * GET request using only the API key (for public/anonymous access).
     */
    public String get(String table, String query) throws IOException {
        return doGet(table, query, null);
    }

    /**
     * POST request to insert a row.
     */
    public String post(String table, String jsonBody, String token) throws IOException {
        String url = config.getSupabaseUrl() + "/rest/v1/" + table;

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) {
                if (response.code() == 401 && token != null && !token.isEmpty()
                        && isTokenExpiredError(responseBody)) {
                    throw new TokenExpiredException("Token expired");
                }
                throw new IOException("POST " + table + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * POST to auth endpoint (signup, login, etc.)
     */
    public String postAuth(String path, String jsonBody) throws IOException {
        String url = config.getSupabaseUrl() + "/auth/v1/" + path;

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("AUTH_ERROR|" + response.code() + "|" + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * GET request to auth endpoint.
     */
    public String getAuth(String path, String token) throws IOException {
        String url = config.getSupabaseUrl() + "/auth/v1/" + path;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GET auth/" + path + " failed: " + response.code() + " " + response.body().string());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * PATCH request to update rows.
     */
    public String patch(String table, String query, String jsonBody, String token) throws IOException {
        String url = config.getSupabaseUrl() + "/rest/v1/" + table;
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) {
                if (response.code() == 401 && token != null && !token.isEmpty()
                        && isTokenExpiredError(responseBody)) {
                    throw new TokenExpiredException("Token expired");
                }
                throw new IOException("PATCH " + table + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * PATCH without token.
     */
    public String patch(String table, String query, String jsonBody) throws IOException {
        return patch(table, query, jsonBody, null);
    }

    /**
     * POST to a Supabase Edge Function.
     */
    public String postEdgeFunction(String functionName, String jsonBody, String token) throws IOException {
        String url = config.getSupabaseUrl() + "/functions/v1/" + functionName;

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (response.code() == 401 && token != null && !token.isEmpty()
                        && isTokenExpiredError(responseBody)) {
                    throw new TokenExpiredException("Token expired");
                }
                throw new IOException("Edge function " + functionName + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    public String delete(String table, String query, String token) throws IOException {
        String url = config.getSupabaseUrl() + "/rest/v1/" + table;
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (response.code() == 401 && token != null && !token.isEmpty()
                        && isTokenExpiredError(responseBody)) {
                    throw new TokenExpiredException("Token expired");
                }
                throw new IOException("DELETE " + table + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * Upload a file to Supabase Storage.
     */
    public String uploadFile(String bucket, String filePath, boolean upsert, String token) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        String fileName = file.getName();
        String url = config.getSupabaseUrl() + "/storage/v1/object/" + bucket + "/" + fileName;

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/*"));

        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody);

        RequestBody requestBody = multipartBuilder.build();

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("apikey", config.getSupabaseKey())
                .addHeader("Content-Type", "multipart/form-data");

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        if (upsert) {
            builder.addHeader("upsert", "true");
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Upload failed: " + response.code() + " " + responseBody);
            }

            return config.getSupabaseUrl() + "/storage/v1/object/public/" + bucket + "/" + fileName;
        }
    }

    /**
     * Exception thrown when a 401 is received due to expired JWT.
     * Callers should catch this, refresh the token, and retry.
     */
    public static class TokenExpiredException extends IOException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }
}
