package com.example.coffeecafe.models;

import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("id")
    private String id;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private String role;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Profile() {}

    public Profile(String id, String fullName, String phone, String role) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAdmin() { return "admin".equals(role); }
    public boolean isShopOwner() { return "shop_owner".equals(role); }
    public boolean isCustomer() { return "customer".equals(role); }
}
