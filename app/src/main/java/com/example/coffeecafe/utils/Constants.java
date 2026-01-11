package com.example.coffeecafe.utils;

public class Constants {
    // Supabase Configuration
    // TODO: Replace with your actual Supabase URL and Anon Key
    public static final String SUPABASE_URL = "YOUR_SUPABASE_URL";
    public static final String SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY";
    
    // Paystack Configuration
    // TODO: Replace with your actual Paystack Public Key
    public static final String PAYSTACK_PUBLIC_KEY = "YOUR_PAYSTACK_PUBLIC_KEY";
    
    // SharedPreferences Keys
    public static final String PREFS_NAME = "CoffeeCafePrefs";
    public static final String PREF_USER_TOKEN = "user_token";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_IS_ADMIN = "is_admin";
    public static final String PREF_CART_ITEMS = "cart_items";
    
    // Database Table Names
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_ORDER_ITEMS = "order_items";
    public static final String TABLE_PAYMENTS = "payments";
    public static final String TABLE_USERS = "users";
    
    // Order Status
    public static final String ORDER_STATUS_PENDING = "pending";
    public static final String ORDER_STATUS_PAID = "paid";
    public static final String ORDER_STATUS_COMPLETED = "completed";
    public static final String ORDER_STATUS_CANCELLED = "cancelled";
    
    // Payment Status
    public static final String PAYMENT_STATUS_PENDING = "pending";
    public static final String PAYMENT_STATUS_SUCCESS = "success";
    public static final String PAYMENT_STATUS_FAILED = "failed";
    
    // Intent Keys
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_PAYMENT_REFERENCE = "payment_reference";
    public static final String EXTRA_TOTAL_AMOUNT = "total_amount";
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}
