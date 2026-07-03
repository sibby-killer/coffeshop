package com.example.coffeecafe.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class CreateAdminFragment extends Fragment {
    private EditText fullNameInput, emailInput, phoneInput, passwordInput;
    private Button createAdminBtn;
    private TextView resultText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fullNameInput = view.findViewById(R.id.admin_fullname);
        emailInput = view.findViewById(R.id.admin_email);
        phoneInput = view.findViewById(R.id.admin_phone);
        passwordInput = view.findViewById(R.id.admin_password);
        createAdminBtn = view.findViewById(R.id.create_admin_btn);
        resultText = view.findViewById(R.id.result_text);

        createAdminBtn.setOnClickListener(v -> createAdmin());
    }

    private void createAdmin() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        createAdminBtn.setEnabled(false);
        createAdminBtn.setText("Creating...");

        new Thread(() -> {
            try {
                // 1. Sign up the admin via Supabase Auth
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("full_name", fullName);
                metadata.put("phone", phone);
                metadata.put("role", "admin");

                Map<String, Object> body = new HashMap<>();
                body.put("email", email);
                body.put("password", password);
                body.put("data", metadata);

                String jsonBody = new Gson().toJson(body);
                String response = SupabaseApi.getInstance().postAuth("signup", jsonBody);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        resultText.setVisibility(View.VISIBLE);
                        resultText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        resultText.setText("Admin account created!\n\n" +
                                "Email: " + email + "\n" +
                                "Password: " + password + "\n\n" +
                                "Send these credentials to the admin. They must verify their email before logging in.");
                        createAdminBtn.setEnabled(false);
                        createAdminBtn.setText("Created");
                        clearFields();
                    });
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                String displayMsg = msg;
                if (msg.contains("AUTH_ERROR")) {
                    String[] parts = msg.split("\\|", 3);
                    if (parts.length >= 3) {
                        try {
                            com.google.gson.JsonObject errorJson = com.google.gson.JsonParser.parseString(parts[2]).getAsJsonObject();
                            displayMsg = errorJson.has("msg") ? errorJson.get("msg").getAsString() : parts[2];
                        } catch (Exception ex) {
                            displayMsg = parts[2];
                        }
                    }
                }

                String finalDisplayMsg = displayMsg;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        resultText.setVisibility(View.VISIBLE);
                        resultText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        resultText.setText("Error: " + finalDisplayMsg);
                        createAdminBtn.setEnabled(true);
                        createAdminBtn.setText("Create Admin Account");
                    });
                }
            }
        }).start();
    }

    private void clearFields() {
        fullNameInput.setText("");
        emailInput.setText("");
        phoneInput.setText("");
        passwordInput.setText("");
    }
}
