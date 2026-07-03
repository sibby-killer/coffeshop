package com.example.coffeecafe.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
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

        new Thread(() -> {
            try {
                String query = "select=*,shops(name)&customer_id=eq." + userId + "&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("orders", query);

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
            holder.orderAmount.setText(String.format("$%.2f", order.getTotalAmount()));
            holder.orderDate.setText(order.getCreatedAt());

            int statusColor;
            switch (order.getStatus()) {
                case "paid": statusColor = 0xFF4CAF50; break;
                case "preparing": statusColor = 0xFFFF9800; break;
                case "ready": statusColor = 0xFF2196F3; break;
                case "completed": statusColor = 0xFF4CAF50; break;
                case "cancelled": statusColor = 0xFFF44336; break;
                default: statusColor = 0xFF9E9E9E; break;
            }
            holder.orderStatus.setTextColor(statusColor);
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.order_id);
                orderStatus = itemView.findViewById(R.id.order_status);
                orderAmount = itemView.findViewById(R.id.order_amount);
                orderDate = itemView.findViewById(R.id.order_date);
            }
        }
    }
}
