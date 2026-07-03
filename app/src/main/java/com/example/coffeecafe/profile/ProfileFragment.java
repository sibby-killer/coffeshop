package com.example.coffeecafe.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.auth.LoginActivity;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Profile;
import com.google.gson.Gson;

public class ProfileFragment extends Fragment {
    private TextView nameText, emailText, roleText, statsText;
    private EditText editName, editPhone;
    private Button saveBtn, logoutBtn;
    private LinearLayout statsCard;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameText = view.findViewById(R.id.profile_name);
        emailText = view.findViewById(R.id.profile_email);
        roleText = view.findViewById(R.id.profile_role);
        editName = view.findViewById(R.id.edit_name);
        editPhone = view.findViewById(R.id.edit_phone);
        saveBtn = view.findViewById(R.id.save_profile_btn);
        logoutBtn = view.findViewById(R.id.logout_btn);
        statsCard = view.findViewById(R.id.stats_card);
        statsText = view.findViewById(R.id.stats_text);
        progressBar = view.findViewById(R.id.progress_bar);

        loadProfile();

        saveBtn.setOnClickListener(v -> saveProfile());
        logoutBtn.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        Profile profile = AuthManager.getInstance(getContext()).getCurrentProfile();

        if (profile != null) {
            nameText.setText(profile.getFullName());
            emailText.setText(AuthManager.getInstance(getContext()).getCurrentEmail());
            roleText.setText(profile.getRole().replace("_", " ").toUpperCase());
            editName.setText(profile.getFullName());
            editPhone.setText(profile.getPhone());

            if (profile.getRole().equals("customer")) {
                loadCustomerStats();
            } else {
                statsCard.setVisibility(View.GONE);
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    private void loadCustomerStats() {
        String token = AuthManager.getInstance(getContext()).getAccessToken();
        String userId = AuthManager.getInstance(getContext()).getCurrentUserId();

        new Thread(() -> {
            try {
                String response = SupabaseApi.getInstance().get("orders",
                        "select=id&customer_id=eq." + userId, token);
                Gson gson = new Gson();
                OrderId[] orders = gson.fromJson(response, OrderId[].class);
                int count = orders != null ? orders.length : 0;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        statsCard.setVisibility(View.VISIBLE);
                        statsText.setText("Total Orders: " + count);
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        statsCard.setVisibility(View.VISIBLE);
                        statsText.setText("Total Orders: 0");
                    });
                }
            }
        }).start();
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();
        String userId = AuthManager.getInstance(getContext()).getCurrentUserId();

        new Thread(() -> {
            try {
                String jsonBody = "{\"full_name\":\"" + name + "\",\"phone\":\"" + phone + "\"}";
                SupabaseApi.getInstance().patch("profiles", "id=eq." + userId, jsonBody, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        nameText.setText(name);
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void logout() {
        AuthManager.getInstance(getContext()).signOut(new AuthManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Logout failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private static class OrderId {
        String id;
    }
}
