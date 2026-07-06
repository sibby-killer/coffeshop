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
import com.example.coffeecafe.utils.CartManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {
    private TextView totalView, itemsSummary, statusText, phoneError, waitingText, waitingSubtext;
    private EditText phoneInput, notesInput;
    private RadioGroup paymentMethodGroup;
    private RadioButton radioMpesa, radioAirtel, radioCard;
    private LinearLayout cardMpesa, cardAirtel, cardCard, formContainer;
    private Button payButton, viewOrderBtn;
    private ProgressBar progressBar;
    private ImageView backBtn;
    private LinearLayout waitingContainer;

    private String createdOrderId;

    private static final String PHONE_ERROR_MESSAGE = "Enter a valid phone number: 254 + 9 digits (e.g. 254712345678)";

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

        // Waiting container (shown after order placed)
        waitingContainer = findViewById(R.id.waiting_container);
        waitingText = findViewById(R.id.waiting_text);
        waitingSubtext = findViewById(R.id.waiting_subtext);
        viewOrderBtn = findViewById(R.id.view_order_btn);

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

        viewOrderBtn.setOnClickListener(v -> {
            if (createdOrderId != null) {
                Intent intent = new Intent(CheckoutActivity.this, OrderTrackingActivity.class);
                intent.putExtra("order_id", createdOrderId);
                startActivity(intent);
                finish();
            }
        });
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

        String paymentMethod;
        if (radioMpesa.isChecked()) {
            paymentMethod = "mobile_money";
        } else if (radioAirtel.isChecked()) {
            paymentMethod = "mobile_money";
        } else {
            paymentMethod = "card";
        }

        if (!radioCard.isChecked()) {
            if (phone.isEmpty()) {
                phoneInput.setError("Phone number required for M-Pesa/Airtel");
                phoneInput.requestFocus();
                return;
            }
            if (!isValidKenyanPhone(phone)) {
                phoneError.setVisibility(View.VISIBLE);
                phoneError.setText(PHONE_ERROR_MESSAGE);
                phoneInput.requestFocus();
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        payButton.setEnabled(false);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Creating order...");

        if (token == null || token.isEmpty()) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                payButton.setEnabled(true);
                statusText.setVisibility(View.GONE);
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            });
            return;
        }

        new Thread(() -> {
            try {
                String shopId = cartManager.getCartShopId();
                final String[] authToken = {AuthManager.getInstance(this).getAccessToken()};

                Map<String, Object> orderData = new HashMap<>();
                orderData.put("customer_id", userId);
                orderData.put("shop_id", shopId);
                orderData.put("total_amount", total);
                orderData.put("status", "pending");
                orderData.put("payment_method", paymentMethod);
                orderData.put("notes", notes);

                String orderJson = new Gson().toJson(orderData);
                String orderResponse;
                try {
                    orderResponse = SupabaseApi.getInstance().post("orders", orderJson, authToken[0]);
                } catch (SupabaseApi.TokenExpiredException e) {
                    if (AuthManager.getInstance(CheckoutActivity.this).refreshAccessToken()) {
                        authToken[0] = AuthManager.getInstance(CheckoutActivity.this).getAccessToken();
                        orderResponse = SupabaseApi.getInstance().post("orders", orderJson, authToken[0]);
                    } else {
                        throw new Exception("Session expired. Please log in again.");
                    }
                }

                Order[] createdOrders = new Gson().fromJson(orderResponse, Order[].class);
                if (createdOrders == null || createdOrders.length == 0) {
                    throw new Exception("Failed to create order");
                }

                String orderId = createdOrders[0].getId();
                createdOrderId = orderId;

                List<CartManager.CartItem> cartItems = cartManager.getCartItems();
                for (CartManager.CartItem item : cartItems) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("order_id", orderId);
                    itemData.put("product_id", item.getProductId());
                    itemData.put("product_name", item.getProductName());
                    itemData.put("quantity", item.getQuantity());
                    itemData.put("price", item.getPrice());

                    String itemJson = new Gson().toJson(itemData);
                    try {
                        SupabaseApi.getInstance().post("order_items", itemJson, authToken[0]);
                    } catch (SupabaseApi.TokenExpiredException e2) {
                        if (AuthManager.getInstance(CheckoutActivity.this).refreshAccessToken()) {
                            authToken[0] = AuthManager.getInstance(CheckoutActivity.this).getAccessToken();
                            SupabaseApi.getInstance().post("order_items", itemJson, authToken[0]);
                        }
                    }
                }

                runOnUiThread(() -> statusText.setText("Initializing payment..."));

                Map<String, Object> paystackBody = new HashMap<>();
                paystackBody.put("amount", total * 100);
                paystackBody.put("email", email);
                paystackBody.put("order_id", orderId);
                paystackBody.put("payment_method", paymentMethod);
                paystackBody.put("callback_url", "https://sibby-killer.github.io/coffeshop/");

                if (!radioCard.isChecked()) {
                    paystackBody.put("phone", phone);
                    if (radioMpesa.isChecked()) {
                        paystackBody.put("provider", "m-pesa");
                    } else {
                        paystackBody.put("provider", "airtel");
                    }
                }

                String responseBody;
                try {
                    responseBody = SupabaseApi.getInstance().postEdgeFunction(
                            "initialize-transaction",
                            new Gson().toJson(paystackBody),
                            authToken[0]
                    );
                } catch (SupabaseApi.TokenExpiredException e) {
                    if (AuthManager.getInstance(CheckoutActivity.this).refreshAccessToken()) {
                        authToken[0] = AuthManager.getInstance(CheckoutActivity.this).getAccessToken();
                        responseBody = SupabaseApi.getInstance().postEdgeFunction(
                                "initialize-transaction",
                                new Gson().toJson(paystackBody),
                                authToken[0]
                        );
                    } else {
                        throw new Exception("Session expired. Please log in again.");
                    }
                }

                JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

                if (responseJson.has("reference")) {
                    String ref = responseJson.get("reference").getAsString();
                    Map<String, Object> refUpdate = new HashMap<>();
                    refUpdate.put("payment_reference", ref);
                    try {
                        SupabaseApi.getInstance().patch("orders", "id=eq." + orderId,
                                new Gson().toJson(refUpdate), authToken[0]);
                    } catch (Exception e) {
                        // Non-critical
                    }
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    cartManager.clearCart();

                    if (responseJson.has("authorization_url")) {
                        String authUrl = responseJson.get("authorization_url").getAsString();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
                        startActivity(browserIntent);

                        showWaitingScreen(orderId);
                    } else {
                        statusText.setText("Payment initiated! Order: " + orderId.substring(0, 8));
                        showWaitingScreen(orderId);
                    }
                });
            } catch (Exception e) {
                String rawError = e.getMessage();
                final String displayError;
                if (rawError != null && rawError.contains("failed:")) {
                    int colonIndex = rawError.indexOf("failed:");
                    displayError = rawError.substring(colonIndex + 7).trim();
                } else {
                    displayError = rawError != null ? rawError : "Unknown error";
                }
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    payButton.setEnabled(true);
                    statusText.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + displayError, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showWaitingScreen(String orderId) {
        formContainer = findViewById(R.id.form_container);
        formContainer.setVisibility(View.GONE);
        waitingContainer.setVisibility(View.VISIBLE);
        String displayId = orderId.length() >= 8 ? orderId.substring(0, 8) : orderId;
        waitingText.setText("Order #" + displayId + " placed!");
        waitingSubtext.setText("Complete your payment in the browser. After payment you'll be redirected back to CoffeeCafe.");
    }
}
