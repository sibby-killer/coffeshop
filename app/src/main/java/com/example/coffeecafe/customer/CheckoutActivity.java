package com.example.coffeecafe.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Order;
import com.example.coffeecafe.models.OrderItem;
import com.example.coffeecafe.utils.CartManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CheckoutActivity extends AppCompatActivity {
    private TextView totalView, itemsSummary, statusText, phoneError;
    private EditText phoneInput, notesInput;
    private RadioGroup paymentMethodGroup;
    private RadioButton radioMpesa, radioAirtel, radioCard;
    private LinearLayout cardMpesa, cardAirtel, cardCard;
    private Button payButton;
    private ProgressBar progressBar;
    private ImageView backBtn;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        totalView = findViewById(R.id.total_view);
        itemsSummary = findViewById(R.id.items_summary);
        phoneInput = findViewById(R.id.phone_input);
        notesInput = findViewById(R.id.notes_input);
        paymentMethodGroup = findViewById(R.id.payment_method_group);
        radioMpesa = findViewById(R.id.payment_mpesa);
        radioAirtel = findViewById(R.id.payment_airtel);
        radioCard = findViewById(R.id.payment_card);
        cardMpesa = findViewById(R.id.card_mpesa);
        cardAirtel = findViewById(R.id.card_airtel);
        cardCard = findViewById(R.id.card_card);
        payButton = findViewById(R.id.pay_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        backBtn = findViewById(R.id.back_btn);
        phoneError = findViewById(R.id.phone_error);

        backBtn.setOnClickListener(v -> finish());

        // Pre-fill phone from profile
        String phone = AuthManager.getInstance(this).getCurrentProfile() != null
                ? AuthManager.getInstance(this).getCurrentProfile().getPhone() : "";
        phoneInput.setText(phone);

        CartManager cartManager = CartManager.getInstance(this);
        List<CartManager.CartItem> items = cartManager.getCartItems();

        double total = cartManager.getCartTotal();
        totalView.setText(String.format("Total: KES %.0f", total));

        StringBuilder summary = new StringBuilder();
        for (CartManager.CartItem item : items) {
            summary.append(item.getProductName())
                    .append(" x").append(item.getQuantity())
                    .append(" - KES ").append(String.format("%.0f", item.getSubtotal()))
                    .append("\n");
        }
        itemsSummary.setText(summary.toString());

        // Card click handlers - select only ONE payment method
        cardMpesa.setOnClickListener(v -> {
            radioMpesa.setChecked(true);
            radioAirtel.setChecked(false);
            radioCard.setChecked(false);
            phoneInput.setVisibility(View.VISIBLE);
        });

        cardAirtel.setOnClickListener(v -> {
            radioMpesa.setChecked(false);
            radioAirtel.setChecked(true);
            radioCard.setChecked(false);
            phoneInput.setVisibility(View.VISIBLE);
        });

        cardCard.setOnClickListener(v -> {
            radioMpesa.setChecked(false);
            radioAirtel.setChecked(false);
            radioCard.setChecked(true);
            phoneInput.setVisibility(View.GONE);
        });

        // Phone validation - live as user types
        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String phoneVal = s.toString().trim();
                if (phoneVal.isEmpty()) {
                    phoneError.setVisibility(View.GONE);
                    return;
                }
                if (!isValidKenyanPhone(phoneVal)) {
                    phoneError.setVisibility(View.VISIBLE);
                    phoneError.setText("Must be 254 + 9 digits (12 total)");
                } else {
                    phoneError.setVisibility(View.GONE);
                }
            }
        });

        payButton.setOnClickListener(v -> processPayment());
    }

    private boolean isValidKenyanPhone(String phone) {
        if (phone.length() != 12) return false;
        if (!phone.startsWith("254")) return false;
        for (int i = 3; i < phone.length(); i++) {
            if (!Character.isDigit(phone.charAt(i))) return false;
        }
        String prefix = phone.substring(3, 5);
        return prefix.equals("70") || prefix.equals("71") || prefix.equals("72") ||
               prefix.equals("73") || prefix.equals("74") || prefix.equals("75") ||
               prefix.equals("76") || prefix.equals("78") || prefix.equals("79") ||
               prefix.equals("10") || prefix.equals("11") || prefix.equals("12") ||
               prefix.equals("68") || prefix.equals("69");
    }

    private void processPayment() {
        CartManager cartManager = CartManager.getInstance(this);
        double total = cartManager.getCartTotal();
        String userId = AuthManager.getInstance(this).getCurrentUserId();
        String email = AuthManager.getInstance(this).getCurrentEmail();
        String token = AuthManager.getInstance(this).getAccessToken();
        String phone = phoneInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Determine payment method
        String paymentMethod;
        if (radioMpesa.isChecked()) {
            paymentMethod = "mobile_money";
        } else if (radioAirtel.isChecked()) {
            paymentMethod = "mobile_money";
        } else {
            paymentMethod = "card";
        }

        // Validate phone for mobile money
        if (!radioCard.isChecked()) {
            if (phone.isEmpty()) {
                phoneInput.setError("Phone number required for M-Pesa/Airtel");
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
        }

        progressBar.setVisibility(View.VISIBLE);
        payButton.setEnabled(false);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Creating order...");

        new Thread(() -> {
            try {
                String shopId = cartManager.getCartShopId();

                // Create order
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("customer_id", userId);
                orderData.put("shop_id", shopId);
                orderData.put("total_amount", total);
                orderData.put("status", "pending");
                orderData.put("payment_method", paymentMethod);
                orderData.put("notes", notes);

                String orderJson = new Gson().toJson(orderData);
                String orderResponse = SupabaseApi.getInstance().post("orders", orderJson, token);

                Order[] createdOrders = new Gson().fromJson(orderResponse, Order[].class);
                if (createdOrders == null || createdOrders.length == 0) {
                    throw new Exception("Failed to create order");
                }

                String orderId = createdOrders[0].getId();

                // Insert order items
                List<CartManager.CartItem> cartItems = cartManager.getCartItems();
                for (CartManager.CartItem item : cartItems) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("order_id", orderId);
                    itemData.put("product_id", item.getProductId());
                    itemData.put("product_name", item.getProductName());
                    itemData.put("quantity", item.getQuantity());
                    itemData.put("price", item.getPrice());

                    String itemJson = new Gson().toJson(itemData);
                    SupabaseApi.getInstance().post("order_items", itemJson, token);
                }

                runOnUiThread(() -> statusText.setText("Initializing payment..."));

                // Call Paystack edge function
                Map<String, Object> paystackBody = new HashMap<>();
                paystackBody.put("amount", total * 100); // Paystack uses kobo/pesewas
                paystackBody.put("email", email);
                paystackBody.put("order_id", orderId);
                paystackBody.put("payment_method", paymentMethod);

                if (!radioCard.isChecked()) {
                    paystackBody.put("phone", phone);
                    if (radioMpesa.isChecked()) {
                        paystackBody.put("provider", "m-pesa");
                    } else {
                        paystackBody.put("provider", "airtel");
                    }
                }

                String responseBody = SupabaseApi.getInstance().postEdgeFunction(
                        "initialize-transaction",
                        new Gson().toJson(paystackBody),
                        token
                );

                // Parse response for redirect URL or authorization URL
                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    cartManager.clearCart();

                    if (responseJson.has("authorization_url")) {
                        String authUrl = responseJson.get("authorization_url").getAsString();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                        startActivity(browserIntent);
                    } else {
                        statusText.setText("Payment initiated! Order: " + orderId.substring(0, 8));
                    }

                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    payButton.setEnabled(true);
                    statusText.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
