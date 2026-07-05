package com.example.coffeecafe.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Random;

public class SignupActivity extends AppCompatActivity {
    private EditText fullNameInput, emailInput, phoneInput, passwordInput;
    private EditText shopNameInput, shopDescInput, shopLocationInput, shopPhoneInput;
    private LinearLayout shopOwnerFields;
    private Spinner roleSpinner;
    private Button signupBtn;
    private TextView loginLink, phoneError, passwordStrength;
    private Toolbar toolbar;

    private String selectedRole = "customer";

    private static final String[] PHONE_ERROR_MESSAGES = {
        "That phone number looks sus... Are you sure that's Kenyan?",
        "12 digits, fam. 254 + 9 digits. You got this!",
        "Even M-Pesa won't recognize that number. Try again!",
        "That's not a phone number, that's a password!",
        "Safaricom called. They said that number doesn't exist.",
        "Airtel says 'nah, try again'. 254XXXXXXXXXX format!",
        "Is that your WiFi password? Use 254 + 9 digits!",
        "That number has trust issues. Too many or too few digits!",
        "Even your grandma's flip phone has a better number!",
        "Error 404: Valid phone number not found. Use 254XXXXXXXXXX!"
    };

    private static final String[] PASSWORD_WEAK_MESSAGES = {
        "Your password is weaker than decaf coffee!",
        "My grandma's cat could crack that password!",
        "That password is as flat as week-old soda!",
        "Even 'password123' is judging you right now!",
        "That password has the security of a wet napkin!",
        "Hackers are literally laughing right now!",
        "That password is more see-through than glass!",
        "Your password just applied for witness protection. It's that weak!",
        "Even a toddler with a keyboard could guess that!",
        "That password is the digital equivalent of a screen door!"
    };

    private static final String[] PASSWORD_MEDIUM_MESSAGES = {
        "Getting there! But still needs more... spice!",
        "Not bad, but your coffee order is stronger!",
        "Almost there! Add some numbers or symbols!",
        "Better than nothing, but we can do better!",
        "That's like a medium roast - needs to be dark roast strong!"
    };

    private static final String[] PASSWORD_STRONG_MESSAGES = {
        "Now THAT'S a password! Strong like Kenyan coffee!",
        "Chef's kiss! That password is fortress-level!",
        "Hackers are crying somewhere. Beautiful password!",
        "That password could guard Fort Knox!",
        "Your password has more layers than an onion!"
    };

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
        phoneError = findViewById(R.id.phone_error);
        passwordStrength = findViewById(R.id.password_strength);

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

        // Phone number validation - live as user types
        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String phone = s.toString().trim();
                if (phone.isEmpty()) {
                    phoneError.setVisibility(View.GONE);
                    return;
                }
                if (!isValidKenyanPhone(phone)) {
                    phoneError.setVisibility(View.VISIBLE);
                    phoneError.setText("Must be 254 + 9 digits (12 total)");
                } else {
                    phoneError.setVisibility(View.GONE);
                }
            }
        });

        // Password strength indicator - live as user types
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                if (password.isEmpty()) {
                    passwordStrength.setVisibility(View.GONE);
                    return;
                }
                passwordStrength.setVisibility(View.VISIBLE);
                int strength = getPasswordStrength(password);
                Random rand = new Random();
                switch (strength) {
                    case 1: // Weak
                        passwordStrength.setTextColor(0xFFFF5252); // Red
                        passwordStrength.setText(PASSWORD_WEAK_MESSAGES[rand.nextInt(PASSWORD_WEAK_MESSAGES.length)]);
                        break;
                    case 2: // Medium
                        passwordStrength.setTextColor(0xFFFF8F00); // Orange
                        passwordStrength.setText(PASSWORD_MEDIUM_MESSAGES[rand.nextInt(PASSWORD_MEDIUM_MESSAGES.length)]);
                        break;
                    case 3: // Strong
                        passwordStrength.setTextColor(0xFF4CAF50); // Green
                        passwordStrength.setText(PASSWORD_STRONG_MESSAGES[rand.nextInt(PASSWORD_STRONG_MESSAGES.length)]);
                        break;
                    default:
                        passwordStrength.setVisibility(View.GONE);
                }
            }
        });
    }

    private boolean isValidKenyanPhone(String phone) {
        if (phone.length() != 12) return false;
        if (!phone.startsWith("254")) return false;
        // Check all characters are digits after 254
        for (int i = 3; i < phone.length(); i++) {
            if (!Character.isDigit(phone.charAt(i))) return false;
        }
        // Check valid Kenyan mobile prefixes after 254
        String prefix = phone.substring(3, 5);
        // Safaricom: 07XX, Airtel: 07XX/01XX, Telkom: 06XX/07XX
        // After 254: 7XX (Safaricom/Airtel), 1XX (Airtel), 6XX (Telkom)
        return prefix.equals("70") || prefix.equals("71") || prefix.equals("72") ||
               prefix.equals("73") || prefix.equals("74") || prefix.equals("75") ||
               prefix.equals("76") || prefix.equals("78") || prefix.equals("79") ||
               prefix.equals("10") || prefix.equals("11") || prefix.equals("12") ||
               prefix.equals("68") || prefix.equals("69");
    }

    private int getPasswordStrength(String password) {
        if (password.length() < 6) return 1; // Weak

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 10) score++;
        if (password.matches(".*[A-Z].*")) score++; // uppercase
        if (password.matches(".*[a-z].*")) score++; // lowercase
        if (password.matches(".*[0-9].*")) score++; // digit
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++; // special char

        if (score <= 2) return 1; // Weak
        if (score <= 4) return 2; // Medium
        return 3; // Strong
    }

    private void attemptSignup() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Full name validation
        if (fullName.isEmpty()) {
            fullNameInput.setError("Enter your name, we don't bite!");
            fullNameInput.requestFocus();
            return;
        }

        // Email validation
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            emailInput.setError("That doesn't look like an email...");
            emailInput.requestFocus();
            return;
        }

        // Phone validation (Kenyan format: 254 + 9 digits = 12 total)
        if (phone.isEmpty()) {
            phoneInput.setError("Phone number is required");
            phoneInput.requestFocus();
            return;
        }
        if (!isValidKenyanPhone(phone)) {
            Random rand = new Random();
            String errorMsg = PHONE_ERROR_MESSAGES[rand.nextInt(PHONE_ERROR_MESSAGES.length)];
            phoneError.setVisibility(View.VISIBLE);
            phoneError.setText(errorMsg);
            phoneInput.requestFocus();
            return;
        }

        // Password validation
        if (password.length() < 6) {
            passwordInput.setError("Password too short! Minimum 6 characters");
            passwordInput.requestFocus();
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

                    String lowerError = error.toLowerCase();
                    if (lowerError.contains("already registered")
                            || lowerError.contains("already exists")
                            || lowerError.contains("user already")
                            || lowerError.contains("email_already_registered")) {
                        showEmailInUseDialog(emailInput.getText().toString().trim());
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void submitShopApplication(Profile profile) {
        String token = AuthManager.getInstance(this).getAccessToken();
        if (token == null || token.isEmpty()) {
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

    private void showEmailInUseDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Email Already Registered")
                .setMessage("An account with this email already exists.\n\nYou can login or reset your password.")
                .setPositiveButton("Login", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setNeutralButton("Reset Password", (dialog, which) -> {
                    dialog.dismiss();
                    AuthManager.getInstance(this).resetPassword(email, new AuthManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Reset link sent! Check your email.", Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
