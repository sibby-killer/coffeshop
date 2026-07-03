package com.example.coffeecafe.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.coffeecafe.BuildConfig;
import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Profile;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginBtn;
    private TextView signupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginBtn = findViewById(R.id.login_button);
        signupLink = findViewById(R.id.signup_link);

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

        loginBtn.setOnClickListener(v -> attemptLogin());
        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        // Forgot Password link
        TextView forgotPasswordLink = findViewById(R.id.forgot_password_link);
        forgotPasswordLink.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email address first", Toast.LENGTH_SHORT).show();
                return;
            }
            showForgotPasswordDialog(email);
        });
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hardcoded admin bypass — skip Supabase entirely
        if (BuildConfig.ADMIN_EMAIL != null && !BuildConfig.ADMIN_EMAIL.isEmpty()
                && email.equals(BuildConfig.ADMIN_EMAIL)
                && password.equals(BuildConfig.ADMIN_PASSWORD)) {
            loginBtn.setEnabled(false);
            loginBtn.setText("Logging in...");
            Toast.makeText(this, "Admin login successful!", Toast.LENGTH_SHORT).show();

            Profile adminProfile = new Profile("admin-local", "Admin", "", "admin");
            AuthManager.getInstance(this).saveLocalSession(adminProfile);

            Intent intent = new Intent(this, com.example.coffeecafe.DashBoard.class);
            intent.putExtra("role", "admin");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Logging in...");

        AuthManager.getInstance(this).signIn(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(Profile profile) {
                if ("shop_owner".equals(profile.getRole())) {
                    submitPendingShopApplication(profile);
                }
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(profile);
                });
            }

            @Override
            public void onEmailNotConfirmed(String emailAddr) {
                runOnUiThread(() -> {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Login");
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Email Not Verified")
                            .setMessage("Please check your email and verify your account before logging in.\n\nDidn't get the email? Check your spam folder.")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Login");
                    Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showForgotPasswordDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("A password reset link will be sent to:\n\n" + email + "\n\nCheck your inbox and follow the instructions.")
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    loginBtn.setEnabled(false);
                    loginBtn.setText("Sending...");

                    AuthManager.getInstance(this).resetPassword(email, new AuthManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                loginBtn.setEnabled(true);
                                loginBtn.setText("Sign In");
                                Toast.makeText(LoginActivity.this, "Reset link sent! Check your email.", Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                loginBtn.setEnabled(true);
                                loginBtn.setText("Sign In");
                                Toast.makeText(LoginActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToDashboard(Profile profile) {
        Intent intent = new Intent(this, com.example.coffeecafe.DashBoard.class);
        intent.putExtra("role", profile.getRole());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void submitPendingShopApplication(Profile profile) {
        SharedPreferences pending = getSharedPreferences("PendingShopApp", MODE_PRIVATE);
        String shopName = pending.getString("shop_name", null);
        if (shopName == null) return;

        new Thread(() -> {
            try {
                String token = AuthManager.getInstance(this).getAccessToken();
                Map<String, Object> body = new HashMap<>();
                body.put("owner_id", profile.getId());
                body.put("shop_name", pending.getString("shop_name", ""));
                body.put("shop_description", pending.getString("shop_description", ""));
                body.put("location", pending.getString("location", ""));
                body.put("phone", pending.getString("phone", ""));
                body.put("status", "pending");

                SupabaseApi.getInstance().post("shop_applications", new Gson().toJson(body), token);
                pending.edit().clear().apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
