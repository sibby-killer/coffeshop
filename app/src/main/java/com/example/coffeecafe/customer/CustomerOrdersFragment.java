package com.example.coffeecafe.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Order;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CustomerOrdersFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private OrderAdapter adapter;
    private List<Order> orderList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.orders_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        orderList = new ArrayList<>();
        adapter = new OrderAdapter(orderList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadOrders();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        String userId = SessionManager.getInstance(getContext()).getUserId();
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*,shops(name)&customer_id=eq." + userId + "&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("orders", query, token);

                Gson gson = new Gson();
                Order[] orders = gson.fromJson(response, Order[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (orders != null && orders.length > 0) {
                            orderList.clear();
                            for (Order order : orders) {
                                orderList.add(order);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setText("Failed to load orders: " + e.getMessage());
                        emptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private void cancelOrder(String orderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Cancel Order", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    String token = AuthManager.getInstance(getContext()).getAccessToken();
                    new Thread(() -> {
                        try {
                            String updateJson = "{\"status\":\"cancelled\"}";
                            SupabaseApi.getInstance().patch("orders", "id=eq." + orderId, updateJson, token);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Order cancelled", Toast.LENGTH_SHORT).show();
                                    loadOrders();
                                });
                            }
                        } catch (Exception e) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    }).start();
                })
                .setNegativeButton("Keep Order", null)
                .show();
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private final List<Order> orders;

        OrderAdapter(List<Order> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orders.get(position);
            String displayId = order.getId() != null && order.getId().length() >= 8
                    ? order.getId().substring(0, 8) : order.getId();
            holder.orderId.setText("Order #" + displayId);
            holder.orderStatus.setText(order.getStatusDisplay());
            holder.orderAmount.setText(String.format("KES %.0f", order.getTotalAmount()));
            holder.orderDate.setText(order.getCreatedAt());

            int statusColor;
            switch (order.getStatus()) {
                case "pending": statusColor = 0xFFFF9800; break;
                case "approved": statusColor = 0xFF2196F3; break;
                case "paid": statusColor = 0xFF4CAF50; break;
                case "preparing": statusColor = 0xFFFF9800; break;
                case "ready": statusColor = 0xFF2196F3; break;
                case "completed": statusColor = 0xFF4CAF50; break;
                case "cancelled": statusColor = 0xFFF44336; break;
                default: statusColor = 0xFF9E9E9E; break;
            }
            holder.orderStatus.setTextColor(statusColor);

            // Show shop name if available
            if (order.getShopName() != null && !order.getShopName().isEmpty()) {
                holder.customerName.setVisibility(View.VISIBLE);
                holder.customerName.setText("From: " + order.getShopName());
            } else {
                holder.customerName.setVisibility(View.GONE);
            }

            // Edit button - only for pending orders
            if ("pending".equals(order.getStatus())) {
                holder.editButton.setVisibility(View.VISIBLE);
                holder.editButton.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), OrderEditActivity.class);
                    intent.putExtra("order_id", order.getId());
                    intent.putExtra("is_shop_owner", false);
                    startActivity(intent);
                });
            } else {
                holder.editButton.setVisibility(View.GONE);
            }

            // Cancel button - only for pending and approved orders
            if ("pending".equals(order.getStatus()) || "approved".equals(order.getStatus())) {
                holder.rejectButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setText("Cancel");
                holder.rejectButton.setOnClickListener(v -> cancelOrder(order.getId()));
            } else {
                holder.rejectButton.setVisibility(View.GONE);
            }

            // No next status button for customers
            holder.nextStatusButton.setVisibility(View.GONE);

            // Click to open order tracking
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), OrderTrackingActivity.class);
                intent.putExtra("order_id", order.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate, customerName;
            Button nextStatusButton, rejectButton, editButton;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.order_id);
                orderStatus = itemView.findViewById(R.id.order_status);
                orderAmount = itemView.findViewById(R.id.order_amount);
                orderDate = itemView.findViewById(R.id.order_date);
                customerName = itemView.findViewById(R.id.customer_name);
                nextStatusButton = itemView.findViewById(R.id.next_status_button);
                rejectButton = itemView.findViewById(R.id.reject_button);
                editButton = itemView.findViewById(R.id.edit_button);
            }
        }
    }
}
