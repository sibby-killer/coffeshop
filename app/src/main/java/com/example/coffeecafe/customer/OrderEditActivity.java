package com.example.coffeecafe.customer;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Order;
import com.example.coffeecafe.models.OrderItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class OrderEditActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView errorText, orderIdText, customerNameText, orderDateText, totalAmountText, orderStatusBadge;
    private View orderInfoCard, totalCard;
    private RecyclerView itemsRecycler;
    private EditText notesInput;
    private Button saveChangesBtn, nextStatusBtn, cancelOrderBtn;
    private View actionBar;

    private String orderId;
    private Order currentOrder;
    private List<EditableOrderItem> editableItems = new ArrayList<>();
    private EditableItemAdapter adapter;
    private boolean isShopOwner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_edit);

        orderId = getIntent().getStringExtra("order_id");
        isShopOwner = getIntent().getBooleanExtra("is_shop_owner", false);

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progress_bar);
        errorText = findViewById(R.id.error_text);
        orderInfoCard = findViewById(R.id.order_info_card);
        orderIdText = findViewById(R.id.order_id_text);
        customerNameText = findViewById(R.id.customer_name_text);
        orderDateText = findViewById(R.id.order_date_text);
        totalAmountText = findViewById(R.id.total_amount_text);
        orderStatusBadge = findViewById(R.id.order_status_badge);
        itemsRecycler = findViewById(R.id.items_recycler);
        notesInput = findViewById(R.id.notes_input);
        saveChangesBtn = findViewById(R.id.save_changes_btn);
        nextStatusBtn = findViewById(R.id.next_status_btn);
        cancelOrderBtn = findViewById(R.id.cancel_order_btn);
        actionBar = findViewById(R.id.action_bar);
        totalCard = findViewById(R.id.total_card);

        itemsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EditableItemAdapter();
        itemsRecycler.setAdapter(adapter);

        saveChangesBtn.setOnClickListener(v -> saveChanges());
        nextStatusBtn.setOnClickListener(v -> advanceStatus());
        cancelOrderBtn.setOnClickListener(v -> cancelOrder());

        loadOrder();
    }

    private void loadOrder() {
        progressBar.setVisibility(View.VISIBLE);
        orderInfoCard.setVisibility(View.GONE);
        totalCard.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String token = AuthManager.getInstance(this).getAccessToken();
                String orderQuery = "id=eq." + orderId + "&select=*,shops(name),profiles(full_name)";
                String orderResponse = SupabaseApi.getInstance().get("orders", orderQuery, token);
                Order[] orders = new Gson().fromJson(orderResponse, Order[].class);

                if (orders == null || orders.length == 0) {
                    runOnUiThread(() -> showError("Order not found"));
                    return;
                }

                currentOrder = orders[0];

                String itemsQuery = "order_id=eq." + orderId;
                String itemsResponse = SupabaseApi.getInstance().get("order_items", itemsQuery, token);
                OrderItem[] items = new Gson().fromJson(itemsResponse, OrderItem[].class);

                editableItems.clear();
                if (items != null) {
                    for (OrderItem item : items) {
                        editableItems.add(new EditableOrderItem(
                                item.getId(), item.getProductId(), item.getProductName(),
                                item.getQuantity(), item.getPrice()));
                    }
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    orderInfoCard.setVisibility(View.VISIBLE);
                    totalCard.setVisibility(View.VISIBLE);

                    String displayId = orderId.length() >= 8 ? orderId.substring(0, 8) : orderId;
                    orderIdText.setText("Order #" + displayId);
                    orderStatusBadge.setText(currentOrder.getStatusDisplay());

                    if (currentOrder.getCustomerName() != null && !currentOrder.getCustomerName().isEmpty()) {
                        customerNameText.setText("Customer: " + currentOrder.getCustomerName());
                        customerNameText.setVisibility(View.VISIBLE);
                    } else {
                        customerNameText.setVisibility(View.GONE);
                    }

                    orderDateText.setText(currentOrder.getCreatedAt());
                    notesInput.setText(currentOrder.getNotes() != null ? currentOrder.getNotes() : "");

                    adapter.notifyDataSetChanged();
                    updateTotal();
                    updateButtons();
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Failed to load order: " + e.getMessage()));
            }
        }).start();
    }

    private void updateTotal() {
        double total = 0;
        for (EditableOrderItem item : editableItems) {
            total += item.price * item.quantity;
        }
        totalAmountText.setText(String.format("KES %.0f", total));
    }

    private void updateButtons() {
        if (currentOrder == null) return;
        String status = currentOrder.getStatus();

        switch (status) {
            case "pending":
                if (isShopOwner) {
                    cancelOrderBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setText("Approve Order");
                    nextStatusBtn.setBackgroundColor(0xFF4CAF50);
                } else {
                    cancelOrderBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setVisibility(View.GONE);
                }
                saveChangesBtn.setVisibility(View.VISIBLE);
                break;
            case "approved":
                if (isShopOwner) {
                    cancelOrderBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setText("Confirm Payment");
                    nextStatusBtn.setBackgroundColor(0xFF4CAF50);
                }
                saveChangesBtn.setVisibility(View.VISIBLE);
                break;
            case "paid":
                if (isShopOwner) {
                    cancelOrderBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setText("Start Preparing");
                    nextStatusBtn.setBackgroundColor(0xFFFF9800);
                }
                saveChangesBtn.setVisibility(View.GONE);
                break;
            case "preparing":
                if (isShopOwner) {
                    nextStatusBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setText("Mark Ready");
                    nextStatusBtn.setBackgroundColor(0xFF2196F3);
                }
                saveChangesBtn.setVisibility(View.GONE);
                cancelOrderBtn.setVisibility(View.GONE);
                break;
            case "ready":
                if (isShopOwner) {
                    nextStatusBtn.setVisibility(View.VISIBLE);
                    nextStatusBtn.setText("Mark Completed");
                    nextStatusBtn.setBackgroundColor(0xFF4CAF50);
                }
                saveChangesBtn.setVisibility(View.GONE);
                cancelOrderBtn.setVisibility(View.GONE);
                break;
            default:
                saveChangesBtn.setVisibility(View.GONE);
                nextStatusBtn.setVisibility(View.GONE);
                cancelOrderBtn.setVisibility(View.GONE);
                break;
        }
    }

    private void saveChanges() {
        if (currentOrder == null) return;

        saveChangesBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String token = AuthManager.getInstance(this).getAccessToken();

                // Save each item's quantity
                for (EditableOrderItem item : editableItems) {
                    if (item.quantity <= 0) {
                        // Delete item
                        SupabaseApi.getInstance().delete("order_items", "id=eq." + item.id, token);
                    } else {
                        // Update quantity
                        String updateJson = "{\"quantity\":" + item.quantity + "}";
                        SupabaseApi.getInstance().patch("order_items", "id=eq." + item.id, updateJson, token);
                    }
                }

                // Recalculate total
                double newTotal = 0;
                for (EditableOrderItem item : editableItems) {
                    if (item.quantity > 0) {
                        newTotal += item.price * item.quantity;
                    }
                }

                // Update order total and notes
                String notes = notesInput.getText().toString().trim();
                String orderUpdate = "{\"total_amount\":" + newTotal + ",\"notes\":\"" + escapeJson(notes) + "\"}";
                SupabaseApi.getInstance().patch("orders", "id=eq." + orderId, orderUpdate, token);

                currentOrder.setTotalAmount(newTotal);
                currentOrder.setNotes(notes);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveChangesBtn.setEnabled(true);
                    Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show();
                    loadOrder();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveChangesBtn.setEnabled(true);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void advanceStatus() {
        if (currentOrder == null) return;

        String newStatus;
        String currentStatus = currentOrder.getStatus();
        switch (currentStatus) {
            case "pending": newStatus = "approved"; break;
            case "approved": newStatus = "paid"; break;
            case "paid": newStatus = "preparing"; break;
            case "preparing": newStatus = "ready"; break;
            case "ready": newStatus = "completed"; break;
            default: return;
        }

        nextStatusBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String token = AuthManager.getInstance(this).getAccessToken();
                String updateJson = "{\"status\":\"" + newStatus + "\"}";
                SupabaseApi.getInstance().patch("orders", "id=eq." + orderId, updateJson, token);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    loadOrder();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    nextStatusBtn.setEnabled(true);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void cancelOrder() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Cancel Order", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    new Thread(() -> {
                        try {
                            String token = AuthManager.getInstance(this).getAccessToken();
                            String updateJson = "{\"status\":\"cancelled\"}";
                            SupabaseApi.getInstance().patch("orders", "id=eq." + orderId, updateJson, token);

                            runOnUiThread(() -> {
                                Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Keep Order", null)
                .show();
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Inner class for editable items
    static class EditableOrderItem {
        String id;
        String productId;
        String name;
        int quantity;
        double price;

        EditableOrderItem(String id, String productId, String name, int quantity, double price) {
            this.id = id;
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }

    // Adapter
    private class EditableItemAdapter extends RecyclerView.Adapter<EditableItemAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_editable, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EditableOrderItem item = editableItems.get(position);
            holder.name.setText(item.name);
            holder.unitPrice.setText(String.format("KES %.0f each", item.price));
            holder.quantity.setText(String.valueOf(item.quantity));
            holder.subtotal.setText(String.format("KES %.0f", item.price * item.quantity));

            holder.qtyMinus.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                    notifyItemChanged(position);
                    updateTotal();
                }
            });

            holder.qtyPlus.setOnClickListener(v -> {
                item.quantity++;
                notifyItemChanged(position);
                updateTotal();
            });

            holder.removeItem.setOnClickListener(v -> {
                editableItems.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, editableItems.size());
                updateTotal();
            });
        }

        @Override
        public int getItemCount() {
            return editableItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, unitPrice, quantity, subtotal;
            Button qtyMinus, qtyPlus, removeItem;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.item_name);
                unitPrice = itemView.findViewById(R.id.item_unit_price);
                quantity = itemView.findViewById(R.id.item_quantity);
                subtotal = itemView.findViewById(R.id.item_subtotal);
                qtyMinus = itemView.findViewById(R.id.qty_minus_btn);
                qtyPlus = itemView.findViewById(R.id.qty_plus_btn);
                removeItem = itemView.findViewById(R.id.remove_item_btn);
            }
        }
    }
}
