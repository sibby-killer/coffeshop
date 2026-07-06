package com.example.coffeecafe.shopowner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class MyShopFragment extends Fragment {
    private EditText shopNameInput, descriptionInput, locationInput, phoneInput;
    private Button saveButton, pickLogoButton;
    private ProgressBar progressBar;
    private TextView titleText, logoStatusText;
    private ImageView shopLogoPreview;
    private String shopId;
    private boolean shopExists = false;
    private Uri selectedLogoUri;
    private String currentImageUrl;

    private static final int PICK_LOGO_REQUEST = 1002;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_confirm_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shopNameInput = view.findViewById(R.id.shop_name_input);
        descriptionInput = view.findViewById(R.id.description_input);
        locationInput = view.findViewById(R.id.location_input);
        phoneInput = view.findViewById(R.id.phone_input);
        saveButton = view.findViewById(R.id.submit_button);
        progressBar = view.findViewById(R.id.progress_bar);
        titleText = view.findViewById(R.id.title);
        pickLogoButton = view.findViewById(R.id.pick_logo_button);
        shopLogoPreview = view.findViewById(R.id.shop_logo_preview);
        logoStatusText = view.findViewById(R.id.logo_status_text);

        saveButton.setOnClickListener(v -> {
            if (shopExists) {
                updateShop();
            } else {
                createShop();
            }
        });

        pickLogoButton.setOnClickListener(v -> openLogoPicker());

        loadShopDetails();
    }

    private void openLogoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Shop Logo"), PICK_LOGO_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_LOGO_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            selectedLogoUri = data.getData();
            shopLogoPreview.setImageURI(selectedLogoUri);
            logoStatusText.setText("Logo selected ✓");
            logoStatusText.setTextColor(0xFF4CAF50);
        }
    }

    private String getFilePathFromUri(Uri uri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = "shop_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";

            File tempFile = new File(getContext().getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.close();
            inputStream.close();
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadShopDetails() {
        String userId = SessionManager.getInstance(getContext()).getUserId();
        String token = AuthManager.getInstance(getContext()).getAccessToken();
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String query = "select=*&owner_id=eq." + userId + "&limit=1";
                String response = SupabaseApi.getInstance().get("shops", query, token);

                Gson gson = new Gson();
                ShopData[] shops = gson.fromJson(response, ShopData[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (shops != null && shops.length > 0) {
                            shopExists = true;
                            ShopData shop = shops[0];
                            shopId = shop.id;
                            currentImageUrl = shop.image_url;
                            titleText.setText("My Shop");
                            shopNameInput.setText(shop.name);
                            descriptionInput.setText(shop.description);
                            locationInput.setText(shop.location);
                            phoneInput.setText(shop.phone);
                            saveButton.setText("Update Shop");

                            // Load existing logo if available
                            if (shop.image_url != null && !shop.image_url.isEmpty()) {
                                logoStatusText.setText("Current logo saved ✓");
                                logoStatusText.setTextColor(0xFF4CAF50);
                            }
                        } else {
                            shopExists = false;
                            titleText.setText("Create Your Shop");
                            saveButton.setText("Create Shop");
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        shopExists = false;
                        titleText.setText("Create Your Shop");
                        saveButton.setText("Create Shop");
                    });
                }
            }
        }).start();
    }

    private void createShop() {
        String name = shopNameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in name, description and location", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        String userId = SessionManager.getInstance(getContext()).getUserId();
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                // Upload logo if selected
                String imageUrl = "";
                if (selectedLogoUri != null) {
                    String filePath = getFilePathFromUri(selectedLogoUri);
                    if (filePath != null) {
                        imageUrl = SupabaseApi.getInstance().uploadFile("shops", filePath, true, token);
                    }
                }

                String json = "{" +
                        "\"owner_id\":\"" + escapeJson(userId) + "\"," +
                        "\"name\":\"" + escapeJson(name) + "\"," +
                        "\"description\":\"" + escapeJson(description) + "\"," +
                        "\"location\":\"" + escapeJson(location) + "\"," +
                        "\"phone\":\"" + escapeJson(phone) + "\"," +
                        "\"image_url\":\"" + escapeJson(imageUrl) + "\"," +
                        "\"is_active\":true" +
                        "}";

                SupabaseApi.getInstance().post("shops", json, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(getContext(), "Shop created!", Toast.LENGTH_SHORT).show();
                        shopExists = true;
                        titleText.setText("My Shop");
                        saveButton.setText("Update Shop");
                        loadShopDetails();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private void updateShop() {
        String name = shopNameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                // Upload new logo if selected
                String imageUrl = currentImageUrl != null ? currentImageUrl : "";
                if (selectedLogoUri != null) {
                    String filePath = getFilePathFromUri(selectedLogoUri);
                    if (filePath != null) {
                        imageUrl = SupabaseApi.getInstance().uploadFile("shops", filePath, true, token);
                    }
                }

                String updateJson = "{\"name\":\"" + escapeJson(name) +
                        "\",\"description\":\"" + escapeJson(description) +
                        "\",\"location\":\"" + escapeJson(location) +
                        "\",\"phone\":\"" + escapeJson(phone) +
                        "\",\"image_url\":\"" + escapeJson(imageUrl) + "\"}";

                SupabaseApi.getInstance().patch("shops", "id=eq." + shopId, updateJson, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(getContext(), "Shop updated!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class ShopData {
        String id, name, description, location, phone, image_url;
    }
}
