package com.example.coffeecafe.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<CartItem> cartItems;
    private CartUpdateListener listener;

    private static final String PREFS_NAME = "CoffeeShopCart";
    private static final String KEY_CART = "cart_items";

    public interface CartUpdateListener {
        void onCartUpdated(List<CartItem> items, double total);
    }

    public static class CartItem {
        private String productId;
        private String shopId;
        private String productName;
        private double price;
        private int quantity;
        private String imageUrl;

        public CartItem(String productId, String shopId, String productName, double price, String imageUrl) {
            this.productId = productId;
            this.shopId = shopId;
            this.productName = productName;
            this.price = price;
            this.imageUrl = imageUrl;
            this.quantity = 1;
        }

        public String getProductId() { return productId; }
        public String getShopId() { return shopId; }
        public String getProductName() { return productName; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getImageUrl() { return imageUrl; }
        public double getSubtotal() { return price * quantity; }
    }

    private CartManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCart();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setCartUpdateListener(CartUpdateListener listener) {
        this.listener = listener;
    }

    private void loadCart() {
        String json = prefs.getString(KEY_CART, null);
        if (json != null) {
            Type type = new TypeToken<List<CartItem>>() {}.getType();
            cartItems = gson.fromJson(json, type);
        } else {
            cartItems = new ArrayList<>();
        }
    }

    private void saveCart() {
        prefs.edit().putString(KEY_CART, gson.toJson(cartItems)).apply();
        notifyListener();
    }

    public void addToCart(CartItem item) {
        for (CartItem existing : cartItems) {
            if (existing.getProductId().equals(item.getProductId())) {
                existing.setQuantity(existing.getQuantity() + 1);
                saveCart();
                return;
            }
        }
        cartItems.add(item);
        saveCart();
    }

    public void removeFromCart(String productId) {
        cartItems.removeIf(item -> item.getProductId().equals(productId));
        saveCart();
    }

    public void updateQuantity(String productId, int quantity) {
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(productId)) {
                if (quantity <= 0) {
                    removeFromCart(productId);
                } else {
                    item.setQuantity(quantity);
                    saveCart();
                }
                return;
            }
        }
    }

    public void clearCart() {
        cartItems.clear();
        saveCart();
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public int getCartCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }

    public double getCartTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }
        return total;
    }

    public boolean isInCart(String productId) {
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(productId)) {
                return true;
            }
        }
        return false;
    }

    public String getCartShopId() {
        if (cartItems.isEmpty()) return null;
        return cartItems.get(0).getShopId();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onCartUpdated(cartItems, getCartTotal());
        }
    }
}
