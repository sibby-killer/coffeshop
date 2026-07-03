package com.example.coffeecafe.models;

import com.google.gson.annotations.SerializedName;

public class ShopApplication {
    @SerializedName("id")
    private String id;

    @SerializedName("owner_id")
    private String ownerId;

    @SerializedName("shop_name")
    private String shopName;

    @SerializedName("shop_description")
    private String shopDescription;

    @SerializedName("location")
    private String location;

    @SerializedName("phone")
    private String phone;

    @SerializedName("status")
    private String status;

    @SerializedName("admin_notes")
    private String adminNotes;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public ShopApplication() {}

    public ShopApplication(String shopName, String shopDescription, String location, String phone) {
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.location = location;
        this.phone = phone;
        this.status = "pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopDescription() { return shopDescription; }
    public void setShopDescription(String shopDescription) { this.shopDescription = shopDescription; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPending() { return "pending".equals(status); }
    public boolean isApproved() { return "approved".equals(status); }
    public boolean isRejected() { return "rejected".equals(status); }
}
