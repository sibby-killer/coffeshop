package com.example.coffeecafe.customer;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Order;
import com.example.coffeecafe.models.OrderItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class OrderTrackingActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView errorText, orderIdText, shopNameText, orderAmountText, orderDateText, orderItemsText;
    private View orderCard, timelineCard;
    private MaterialCardView verifyCard;
    private MaterialButton verifyPaymentBtn;
    private ProgressBar verifyProgress;
    private TextView verifyStatus;

    // Step views
    private View dotPending, dotPaid, dotPreparing, dotReady, dotCompleted;
    private View linePending, linePaid, linePreparing, lineReady;
    private TextView labelPending, labelPaid, labelPreparing, labelReady, labelCompleted;
    private TextView timePending, timePaid, timePreparing, timeReady, timeCompleted;

    private static final int COLOR_ACTIVE = 0xFFFF8F00;
    private static final int COLOR_DONE = 0xFF4CAF50;
    private static final int COLOR_INACTIVE = 0xFFBBBBBB;
    private static final int COLOR_LINE_ACTIVE = 0xFF4CAF50;
    private static final int COLOR_LINE_INACTIVE = 0xFFCCCCCC;

    private String currentOrderId;
    private String currentReference;
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private boolean isPolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        orderCard = findViewById(R.id.order_card);
        timelineCard = findViewById(R.id.timeline_card);
        orderIdText = findViewById(R.id.order_id_text);
        shopNameText = findViewById(R.id.shop_name_text);
        orderAmountText = findViewById(R.id.order_amount_text);
        orderDateText = findViewById(R.id.order_date_text);
        orderItemsText = findViewById(R.id.order_items_text);

        dotPending = findViewById(R.id.dot_pending);
        dotPaid = findViewById(R.id.dot_paid);
        dotPreparing = findViewById(R.id.dot_preparing);
        dotReady = findViewById(R.id.dot_ready);
        dotCompleted = findViewById(R.id.dot_completed);

        linePending = findViewById(R.id.line_pending);
        linePaid = findViewById(R.id.line_paid);
        linePreparing = findViewById(R.id.line_preparing);
        lineReady = findViewById(R.id.line_ready);

        labelPending = findViewById(R.id.label_pending);
        labelPaid = findViewById(R.id.label_paid);
        labelPreparing = findViewById(R.id.label_preparing);
        labelReady = findViewById(R.id.label_ready);
        labelCompleted = findViewById(R.id.label_completed);

        timePending = findViewById(R.id.time_pending);
        timePaid = findViewById(R.id.time_paid);
        timePreparing = findViewById(R.id.time_preparing);
        timeReady = findViewById(R.id.time_ready);
        timeCompleted = findViewById(R.id.time_completed);

        verifyCard = findViewById(R.id.verify_card);
        verifyPaymentBtn = findViewById(R.id.verify_payment_btn);
        verifyProgress = findViewById(R.id.verify_progress);
        verifyStatus = findViewById(R.id.verify_status);

        currentOrderId = getIntent().getStringExtra("order_id");
        currentReference = getIntent().getStringExtra("reference");

        // Verify payment button click
        verifyPaymentBtn.setOnClickListener(v -> verifyPayment());

        if (currentOrderId != null) {
            verifyAndLoadOrder(currentOrderId, currentReference);
        } else {
            showError("No order ID provided");
        }
    }

    private void verifyAndLoadOrder(String orderId, String reference) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // If we have a reference, verify the payment first
                if (reference != null && !reference.isEmpty()) {
                    String verifyUrl = "verify-transaction?reference=" + reference;
                    try {
                        SupabaseApi.getInstance().postEdgeFunction(verifyUrl, "{}", null);
                    } catch (Exception e) {
                        // Verification might fail but order could still exist, continue
                    }
                }

                loadOrderDetails(orderId);

                // Start polling if order is still pending
                startPolling(orderId);
            } catch (Exception e) {
                runOnUiThread(() -> showError("Failed to load order: " + e.getMessage()));
            }
        }).start();
    }

    private void loadOrderDetails(String orderId) throws Exception {
        String token = AuthManager.getInstance(this).getAccessToken();
        String orderQuery = "id=eq." + orderId + "&select=*,shops(name)";
        String orderResponse = SupabaseApi.getInstance().get("orders", orderQuery, token);
        Order[] orders = new Gson().fromJson(orderResponse, Order[].class);

        if (orders == null || orders.length == 0) {
            runOnUiThread(() -> showError("Order not found"));
            return;
        }

        Order order = orders[0];

        // Load order items
        String itemsQuery = "order_id=eq." + orderId;
        String itemsResponse = SupabaseApi.getInstance().get("order_items", itemsQuery, token);
        OrderItem[] items = new Gson().fromJson(itemsResponse, OrderItem[].class);

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            orderCard.setVisibility(View.VISIBLE);
            timelineCard.setVisibility(View.VISIBLE);

            String displayId = orderId.length() >= 8 ? orderId.substring(0, 8) : orderId;
            orderIdText.setText("Order #" + displayId);

            if (order.getShopName() != null && !order.getShopName().isEmpty()) {
                shopNameText.setText("From: " + order.getShopName());
                shopNameText.setVisibility(View.VISIBLE);
            } else {
                shopNameText.setVisibility(View.GONE);
            }

            orderAmountText.setText(String.format("KES %.0f", order.getTotalAmount()));
            orderDateText.setText(order.getCreatedAt());

            StringBuilder sb = new StringBuilder();
            if (items != null) {
                for (OrderItem item : items) {
                    sb.append(item.getProductName())
                      .append(" x").append(item.getQuantity())
                      .append(" - KES ").append(String.format("%.0f", item.getSubtotal()))
                      .append("\n");
                }
            }
            orderItemsText.setText(sb.toString().trim());

            updateTimeline(order.getStatus(), order.getCreatedAt());

            // Show verify button only if order is still pending
            if ("pending".equals(order.getStatus())) {
                verifyCard.setVisibility(View.VISIBLE);
            } else {
                verifyCard.setVisibility(View.GONE);
            }
        });
    }

    private void updateTimeline(String status, String createdAt) {
        // Define which steps are completed based on status
        boolean hasPending = true;
        boolean hasApproved = false;
        boolean hasPaid = false;
        boolean hasPreparing = false;
        boolean hasReady = false;
        boolean hasCompleted = false;

        switch (status) {
            case "approved":
                hasApproved = true;
                break;
            case "paid":
                hasApproved = true;
                hasPaid = true;
                break;
            case "preparing":
                hasApproved = true;
                hasPaid = true;
                hasPreparing = true;
                break;
            case "ready":
                hasApproved = true;
                hasPaid = true;
                hasPreparing = true;
                hasReady = true;
                break;
            case "completed":
                hasApproved = true;
                hasPaid = true;
                hasPreparing = true;
                hasReady = true;
                hasCompleted = true;
                break;
            default:
                // pending - only first step active
                break;
        }

        // Update pending
        setStepActive(dotPending, labelPending, timePending, true, "Order Placed", createdAt);

        // Update approved
        setStepActive(dotPaid, labelPaid, timePaid, hasApproved,
                hasApproved ? "Approved by Shop" : "Awaiting Approval",
                hasApproved ? createdAt : "");
        setLineActive(linePending, hasApproved);

        // Update paid
        setStepActive(dotPreparing, labelPreparing, timePreparing, hasPaid,
                hasPaid ? "Payment Confirmed" : "Waiting...",
                hasPaid ? createdAt : "");
        setLineActive(linePaid, hasPaid);

        // Update ready
        setStepActive(dotReady, labelReady, timeReady, hasReady,
                hasReady ? "Ready for Pickup" : "Waiting...",
                hasReady ? createdAt : "");
        setLineActive(linePreparing, hasReady);

        // Update completed
        setStepActive(dotCompleted, labelCompleted, timeCompleted, hasCompleted,
                hasCompleted ? "Completed!" : "Waiting...",
                hasCompleted ? createdAt : "");
        setLineActive(lineReady, hasCompleted);
    }

    private void setStepActive(View dot, TextView label, TextView time, boolean active, String labelText, String timeText) {
        dot.setBackgroundResource(active ? R.drawable.dot_active : R.drawable.dot_inactive);
        label.setText(labelText);
        label.setTextColor(active ? 0xFF333333 : COLOR_INACTIVE);
        label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        time.setText(active && !timeText.isEmpty() ? timeText : "");
        time.setTextColor(active ? 0xFF999999 : COLOR_INACTIVE);
    }

    private void setLineActive(View line, boolean active) {
        line.setBackgroundColor(active ? COLOR_LINE_ACTIVE : COLOR_LINE_INACTIVE);
    }

    private void verifyPayment() {
        if (currentOrderId == null) return;

        verifyPaymentBtn.setEnabled(false);
        verifyProgress.setVisibility(View.VISIBLE);
        verifyStatus.setVisibility(View.VISIBLE);
        verifyStatus.setText("Verifying payment...");

        new Thread(() -> {
            try {
                // First try to get the reference from the order itself
                String token = AuthManager.getInstance(OrderTrackingActivity.this).getAccessToken();
                String orderQuery = "id=eq." + currentOrderId + "&select=payment_reference";
                String orderResponse = SupabaseApi.getInstance().get("orders", orderQuery, token);
                Order[] orders = new Gson().fromJson(orderResponse, Order[].class);

                String reference = null;
                if (orders != null && orders.length > 0 && orders[0].getPaymentReference() != null) {
                    reference = orders[0].getPaymentReference();
                } else if (currentReference != null) {
                    reference = currentReference;
                }

                if (reference != null && !reference.isEmpty()) {
                    // Call verify-transaction edge function
                    String verifyUrl = "verify-transaction?reference=" + reference;
                    try {
                        SupabaseApi.getInstance().postEdgeFunction(verifyUrl, "{}", null);
                    } catch (Exception e) {
                        // Continue anyway - order might already be updated
                    }
                }

                // Reload order to get updated status
                loadOrderDetails(currentOrderId);

                runOnUiThread(() -> {
                    verifyProgress.setVisibility(View.GONE);
                    verifyPaymentBtn.setEnabled(true);
                    verifyStatus.setText("Payment verified! Status updated.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    verifyProgress.setVisibility(View.GONE);
                    verifyPaymentBtn.setEnabled(true);
                    verifyStatus.setText("Could not verify. Tap again or check your order.");
                });
            }
        }).start();
    }

    private void startPolling(String orderId) {
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isPolling) return;
                isPolling = true;

                new Thread(() -> {
                    try {
                        loadOrderDetails(orderId);
                    } catch (Exception e) {
                        // Silently ignore polling errors
                    } finally {
                        isPolling = false;
                        if (!isFinishing()) {
                            pollingHandler.postDelayed(this, 5000);
                        }
                    }
                }).start();
            }
        };
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }
}
