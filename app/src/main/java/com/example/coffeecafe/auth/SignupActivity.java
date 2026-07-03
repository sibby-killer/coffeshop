package com.example.coffeecafe.auth;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Profile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private EditText fullNameInput, emailInput, phoneInput, passwordInput;
    private EditText shopNameInput, shopDescInput, shopLocationInput, shopPhoneInput;
    private LinearLayout shopOwnerFields;
    private Spinner roleSpinner;
    private Button signupBtn;
    private TextView loginLink;
    private Toolbar toolbar;

    private String selectedRole = "customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        fullNameInput = findViewById(R.id.full_name_input);
        emailInput = findViewById(R.id.email_input);
        phoneInput = findViewById(R.id.phone_input);
        passwordInput = findViewById(R.id.password_input);
        shopNameInput = findViewById(R.id.shop_name_input);
        shopDescInput = findViewById(R.id.shop_description_input);
        shopLocationInput = findViewById(R.id.shop_location_input);
        shopPhoneInput = findViewById(R.id.shop_phone_input);
        shopOwnerFields = findViewById(R.id.shop_owner_fields);
        roleSpinner = findViewById(R.id.role_spinner);
        signupBtn = findViewById(R.id.signup_button);
        loginLink = findViewById(R.id.login_link);

        // Role spinner setup
        String[] roles = {"I want to order (Customer)", "I want to sell (Shop Owner)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedRole = "customer";
                    shopOwnerFields.setVisibility(View.GONE);
                } else {
                    selectedRole = "shop_owner";
                    shopOwnerFields.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        signupBtn.setOnClickListener(v -> attemptSignup());
        loginLink.setOnClickListener(v -> finish());

        // Password visibility toggle
        ImageView togglePassword = findViewById(R.id.toggle_password);
        togglePassword.setOnClickListener(v -> {
            if (passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod) {
                passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_eye_on);
            } else {
                passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_eye_off);
            }
            passwordInput.setSelection(passwordInput.getText().length());
        });
    }

    private void attemptSignup() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate shop owner fields
        if (selectedRole.equals("shop_owner")) {
            String shopName = shopNameInput.getText().toString().trim();
            String shopDesc = shopDescInput.getText().toString().trim();
            String shopLocation = shopLocationInput.getText().toString().trim();

            if (shopName.isEmpty() || shopDesc.isEmpty() || shopLocation.isEmpty()) {
                Toast.makeText(this, "Please fill in shop name, description, and location", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        signupBtn.setEnabled(false);
        signupBtn.setText("Creating account...");

        AuthManager.getInstance(this).signUp(email, password, fullName, phone, selectedRole, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(Profile profile) {
                // If shop owner, also submit the application
                if (selectedRole.equals("shop_owner")) {
                    submitShopApplication(profile);
                } else {
                    runOnUiThread(() -> showEmailVerificationDialog(email));
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    signupBtn.setEnabled(true);
                    signupBtn.setText("Create Account");
                    Toast.makeText(SignupActivity.this, "Signup failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void submitShopApplication(Profile profile) {
        String token = AuthManager.getInstance(this).getAccessToken();
        if (token == null || token.isEmpty()) {
            // Email confirmation required — save shop data locally, submit after first login
            savePendingShopApplication(profile.getId());
            runOnUiThread(() -> showEmailVerificationDialog(emailInput.getText().toString().trim()));
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("owner_id", profile.getId());
                body.put("shop_name", shopNameInput.getText().toString().trim());
                body.put("shop_description", shopDescInput.getText().toString().trim());
                body.put("location", shopLocationInput.getText().toString().trim());
                body.put("phone", shopPhoneInput.getText().toString().trim());
                body.put("status", "pending");

                String jsonBody = new Gson().toJson(body);
                SupabaseApi.getInstance().post("shop_applications", jsonBody, token);

                runOnUiThread(() -> showEmailVerificationDialog(emailInput.getText().toString().trim()));
            } catch (Exception e) {
                // Even if POST fails, save locally and still show verification dialog
                savePendingShopApplication(profile.getId());
                runOnUiThread(() -> showEmailVerificationDialog(emailInput.getText().toString().trim()));
            }
        }).start();
    }

    private void savePendingShopApplication(String ownerId) {
        getSharedPreferences("PendingShopApp", MODE_PRIVATE).edit()
                .putString("owner_id", ownerId)
                .putString("shop_name", shopNameInput.getText().toString().trim())
                .putString("shop_description", shopDescInput.getText().toString().trim())
                .putString("location", shopLocationInput.getText().toString().trim())
                .putString("phone", shopPhoneInput.getText().toString().trim())
                .apply();
    }

    private void showEmailVerificationDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Check Your Email")
                .setMessage("We sent a verification link to:\n\n" + email + "\n\nPlease check your inbox and click the link to verify your account, then come back and login.")
                .setPositiveButton("Go to Login", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
