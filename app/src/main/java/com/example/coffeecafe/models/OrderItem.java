package com.example.coffeecafe.models;

import com.google.gson.annotations.SerializedName;

public class OrderItem {
    @SerializedName("id")
    private String id;

    @SerializedName("order_id")
    private String orderId;

    @SerializedName("product_id")
    private String productId;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("price")
    private double price;

    @SerializedName("created_at")
    private String createdAt;

    public OrderItem() {}

    public OrderItem(String productId, String productName, int quantity, double price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public double getSubtotal() {
        return price * quantity;
    }
}
