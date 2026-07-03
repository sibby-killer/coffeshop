package com.example.coffeecafe.shopowner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

public class MyShopFragment extends Fragment {
    private EditText shopNameInput, descriptionInput, locationInput, phoneInput;
    private Button saveButton;
    private ProgressBar progressBar;
    private String shopId;

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

        saveButton.setText("Save Changes");
        saveButton.setOnClickListener(v -> saveShop());

        loadShopDetails();
    }

    private void loadShopDetails() {
        String userId = SessionManager.getInstance(getContext()).getUserId();
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String query = "select=*&owner_id=eq." + userId + "&limit=1";
                String response = SupabaseApi.getInstance().get("shops", query);

                Gson gson = new Gson();
                ShopData[] shops = gson.fromJson(response, ShopData[].class);

                if (getActivity() != null && shops != null && shops.length > 0) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        ShopData shop = shops[0];
                        shopId = shop.id;
                        shopNameInput.setText(shop.name);
                        descriptionInput.setText(shop.description);
                        locationInput.setText(shop.location);
                        phoneInput.setText(shop.phone);
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }
        }).start();
    }

    private void saveShop() {
        String name = shopNameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || location.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        new Thread(() -> {
            try {
                String updateJson = "{\"name\":\"" + escapeJson(name) +
                        "\",\"description\":\"" + escapeJson(description) +
                        "\",\"location\":\"" + escapeJson(location) +
                        "\",\"phone\":\"" + escapeJson(phone) + "\"}";

                SupabaseApi.getInstance().patch("shops", "id=eq." + shopId, updateJson);

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
        String id, name, description, location, phone;
    }
}
