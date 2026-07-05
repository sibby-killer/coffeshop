package com.example.coffeecafe.config;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Singleton helper for making Supabase REST API calls via OkHttp.
 * Handles GET, POST, PATCH, and DELETE operations against the PostgREST and Auth APIs.
 */
public class SupabaseApi {
    private static SupabaseApi instance;
    private final SupabaseConfig config;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

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
     * GET request to PostgREST endpoint.
     * @param table Table name (e.g. "shops")
     * @param query Query parameters (e.g. "select=*&is_active=eq.true")
     * @param token Bearer token for authorization (can be null for public reads)
     * @return Response body string
     */
    public String get(String table, String query, String token) throws IOException {
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
                throw new IOException("GET " + table + " failed: " + response.code() + " " + body);
            }
            return body;
        }
    }

    /**
     * GET request using only the API key (for public/anonymous access).
     */
    public String get(String table, String query) throws IOException {
        return get(table, query, null);
    }

    /**
     * POST request to insert a row.
     * @param table Table name
     * @param jsonBody JSON body string
     * @param token Bearer token (can be null)
     * @return Response body string
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
                throw new IOException("POST " + table + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * POST to auth endpoint (signup, login, etc.)
     * Returns the response body. On non-200, throws IOException with format:
     *   "AUTH_ERROR|{statusCode}|{responseBody}"
     * Callers can parse the error body from the exception message.
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
     * GET request to auth endpoint (e.g. get user info).
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
     * @param table Table name
     * @param query Query filter (e.g. "id=eq.123")
     * @param jsonBody JSON body with update fields
     * @param token Bearer token (can be null)
     * @return Response body string
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
                throw new IOException("PATCH " + table + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * PATCH without token (using only API key).
     */
    public String patch(String table, String query, String jsonBody) throws IOException {
        return patch(table, query, jsonBody, null);
    }

    /**
     * POST to a Supabase Edge Function.
     * @param functionName Edge function name
     * @param jsonBody JSON body
     * @param token Bearer token
     * @return Response body string
     */
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
                throw new IOException("DELETE " + table + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

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
                throw new IOException("Edge function " + functionName + " failed: " + response.code() + " " + responseBody);
            }
            return responseBody;
        }
    }

    /**
     * Upload a file to Supabase Storage.
     * @param bucket Storage bucket name (e.g. "products", "shops")
     * @param filePath Local file path to upload
     * @param upsert If true, overwrite existing file with same name
     * @param token Bearer token for authorization
     * @return Public URL of the uploaded file
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

            // Construct public URL
            return config.getSupabaseUrl() + "/storage/v1/object/public/" + bucket + "/" + fileName;
        }
    }
}
