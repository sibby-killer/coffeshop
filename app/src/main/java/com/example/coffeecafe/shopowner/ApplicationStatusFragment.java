package com.example.coffeecafe.shopowner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.ShopApplication;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

public class ApplicationStatusFragment extends Fragment {
    private EditText shopNameInput, descriptionInput, locationInput, phoneInput;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextView statusView, statusMessage;

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
        submitButton = view.findViewById(R.id.submit_button);
        progressBar = view.findViewById(R.id.progress_bar);
        statusView = view.findViewById(R.id.status_view);
        statusMessage = view.findViewById(R.id.status_message);

        submitButton.setOnClickListener(v -> submitApplication());

        checkExistingApplication();
    }

    private void checkExistingApplication() {
        String userId = SessionManager.getInstance(getContext()).getUserId();
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String query = "select=*&owner_id=eq." + userId + "&order=created_at.desc&limit=1";
                String response = SupabaseApi.getInstance().get("shop_applications", query);

                Gson gson = new Gson();
                ShopApplication[] apps = gson.fromJson(response, ShopApplication[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (apps != null && apps.length > 0) {
                            showApplicationStatus(apps[0]);
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }
        }).start();
    }

    private void showApplicationStatus(ShopApplication app) {
        shopNameInput.setVisibility(View.GONE);
        descriptionInput.setVisibility(View.GONE);
        locationInput.setVisibility(View.GONE);
        phoneInput.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);

        statusView.setVisibility(View.VISIBLE);
        statusView.setText("Application Status: " + app.getStatus().toUpperCase());
        statusMessage.setVisibility(View.VISIBLE);
        statusMessage.setText("Shop Name: " + app.getShopName());
    }

    private void submitApplication() {
        String shopName = shopNameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (shopName.isEmpty() || description.isEmpty() || location.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        String userId = SessionManager.getInstance(getContext()).getUserId();

        new Thread(() -> {
            try {
                ShopApplication app = new ShopApplication(shopName, description, location, phone);
                app.setOwnerId(userId);

                String json = new Gson().toJson(app);

                SupabaseApi.getInstance().post("shop_applications", json, null);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Application submitted!", Toast.LENGTH_SHORT).show();
                        checkExistingApplication();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }
}
