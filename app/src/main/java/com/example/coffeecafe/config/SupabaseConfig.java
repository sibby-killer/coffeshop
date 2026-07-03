package com.example.coffeecafe.config;

import com.example.coffeecafe.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Supabase configuration - holds URL, API key, and provides an OkHttpClient.
 * All Supabase interactions use direct REST API calls via OkHttp.
 */
public class SupabaseConfig {
    private static SupabaseConfig instance;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final OkHttpClient httpClient;

    private SupabaseConfig() {
        supabaseUrl = BuildConfig.SUPABASE_URL;
        supabaseKey = BuildConfig.SUPABASE_KEY;

        if (supabaseUrl == null || supabaseUrl.isEmpty() || supabaseKey == null || supabaseKey.isEmpty()) {
            throw new IllegalStateException(
                    "Supabase credentials not configured. Set SUPABASE_URL and SUPABASE_KEY in local.properties"
            );
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized SupabaseConfig getInstance() {
        if (instance == null) {
            instance = new SupabaseConfig();
        }
        return instance;
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getSupabaseKey() {
        return supabaseKey;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }
}
