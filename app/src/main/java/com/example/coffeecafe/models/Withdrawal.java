package com.example.coffeecafe.models;

import com.google.gson.annotations.SerializedName;

public class Withdrawal {
    @SerializedName("id")
    private String id;

    @SerializedName("shop_owner_id")
    private String shopOwnerId;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("account_number")
    private String accountNumber;

    @SerializedName("amount")
    private double amount;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    public Withdrawal() {}

    public Withdrawal(String shopOwnerId, String fullName, String paymentMethod, String accountNumber, double amount) {
        this.shopOwnerId = shopOwnerId;
        this.fullName = fullName;
        this.paymentMethod = paymentMethod;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.status = "pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getShopOwnerId() { return shopOwnerId; }
    public void setShopOwnerId(String shopOwnerId) { this.shopOwnerId = shopOwnerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStatusDisplay() {
        switch (status) {
            case "pending": return "Pending";
            case "approved": return "Approved";
            case "rejected": return "Rejected";
            case "completed": return "Completed";
            default: return status;
        }
    }
}
