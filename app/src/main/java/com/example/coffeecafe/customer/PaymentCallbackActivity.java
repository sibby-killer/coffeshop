package com.example.coffeecafe.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.utils.CartManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class PaymentCallbackActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_callback);

        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        String orderId = getIntent().getStringExtra("order_id");
        String reference = getIntent().getStringExtra("reference");

        if (orderId == null || orderId.isEmpty()) {
            // Try extracting from URI
            if (getIntent().getData() != null) {
                orderId = getIntent().getData().getQueryParameter("order_id");
                reference = getIntent().getData().getQueryParameter("reference");
            }
        }

        if (orderId != null) {
            verifyPayment(orderId, reference);
        } else {
            statusText.setText("No order to verify");
            statusText.setTextColor(0xFFF44336);
        }
    }

    private void verifyPayment(String orderId, String reference) {
        statusText.setText("Verifying your payment...");
        progressBar.setVisibility(android.view.View.VISIBLE);

        new Thread(() -> {
            try {
                if (reference != null && !reference.isEmpty()) {
                    // Call verify-transaction edge function
                    Map<String, Object> body = new HashMap<>();
                    body.put("reference", reference);
                    String response = SupabaseApi.getInstance().postEdgeFunction(
                            "verify-transaction?reference=" + reference,
                            "{}",
                            null
                    );

                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    String paymentStatus = json.has("data") ?
                            json.getAsJsonObject("data").get("status").getAsString() : "unknown";

                    runOnUiThread(() -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        CartManager.getInstance(this).clearCart();

                        if ("success".equals(paymentStatus)) {
                            statusText.setText("Payment successful!");
                            statusText.setTextColor(0xFF4CAF50);
                        } else {
                            statusText.setText("Payment " + paymentStatus);
                            statusText.setTextColor(0xFFFF9800);
                        }

                        // Navigate to order tracking after 1.5 seconds
                        statusText.postDelayed(() -> {
                            Intent intent = new Intent(this, OrderTrackingActivity.class);
                            intent.putExtra("order_id", orderId);
                            startActivity(intent);
                            finish();
                        }, 1500);
                    });
                } else {
                    // No reference, just go to order tracking
                    runOnUiThread(() -> {
                        progressBar.setVisibility(android.view.View.GONE);
                        CartManager.getInstance(this).clearCart();
                        Intent intent = new Intent(this, OrderTrackingActivity.class);
                        intent.putExtra("order_id", orderId);
                        startActivity(intent);
                        finish();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    CartManager.getInstance(this).clearCart();
                    // Even if verification fails, show the order
                    statusText.setText("Payment submitted. Viewing order...");
                    statusText.setTextColor(0xFFFF9800);

                    statusText.postDelayed(() -> {
                        Intent intent = new Intent(this, OrderTrackingActivity.class);
                        intent.putExtra("order_id", orderId);
                        startActivity(intent);
                        finish();
                    }, 1500);
                });
            }
        }).start();
    }
}
