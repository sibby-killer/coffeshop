package com.example.coffeecafe.models;

import com.google.gson.annotations.SerializedName;

public class Order {
    @SerializedName("id")
    private String id;

    @SerializedName("customer_id")
    private String customerId;

    @SerializedName("shop_id")
    private String shopId;

    @SerializedName("total_amount")
    private double totalAmount;

    @SerializedName("status")
    private String status;

    @SerializedName("payment_reference")
    private String paymentReference;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("notes")
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Transient fields (not from DB, joined from other tables)
    private transient String shopName;
    private transient String customerName;

    public Order() {}

    public Order(String customerId, String shopId, double totalAmount) {
        this.customerId = customerId;
        this.shopId = shopId;
        this.totalAmount = totalAmount;
        this.status = "pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isPaid() { return "paid".equals(status); }
    public boolean isPreparing() { return "preparing".equals(status); }
    public boolean isReady() { return "ready".equals(status); }
    public boolean isCompleted() { return "completed".equals(status); }
    public boolean isCancelled() { return "cancelled".equals(status); }

    public String getStatusDisplay() {
        switch (status) {
            case "pending": return "Pending Payment";
            case "paid": return "Payment Confirmed";
            case "preparing": return "Preparing";
            case "ready": return "Ready for Pickup";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            default: return status;
        }
    }
}
