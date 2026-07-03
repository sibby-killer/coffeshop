package com.example.coffeecafe.models;

import com.google.gson.annotations.SerializedName;

public class Shop {
    @SerializedName("id")
    private String id;

    @SerializedName("application_id")
    private String applicationId;

    @SerializedName("owner_id")
    private String ownerId;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("location")
    private String location;

    @SerializedName("phone")
    private String phone;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Shop() {}

    public Shop(String id, String name, String description, String location, String phone) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.phone = phone;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
