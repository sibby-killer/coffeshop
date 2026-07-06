package com.example.coffeecafe.shopowner;

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

public class ShopOrdersFragment extends Fragment {
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
        adapter = new OrderAdapter(orderList, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onStatusUpdate(String orderId, String newStatus) {
                updateOrderStatus(orderId, newStatus);
            }
        });

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
                String shopId = getShopId(userId);
                if (shopId.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            emptyView.setText("No shop found");
                            emptyView.setVisibility(View.VISIBLE);
                        });
                    }
                    return;
                }

                String token = AuthManager.getInstance(getContext()).getAccessToken();
                String query = "select=*,profiles(full_name)&shop_id=eq." + shopId + "&order=created_at.desc";
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

    private String getShopId(String ownerId) {
        try {
            String query = "select=id&owner_id=eq." + ownerId + "&limit=1";
            String response = SupabaseApi.getInstance().get("shops", query);

            Gson gson = new Gson();
            ShopId[] shops = gson.fromJson(response, ShopId[].class);
            if (shops != null && shops.length > 0) {
                return shops[0].id;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static class ShopId {
        String id;
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        new Thread(() -> {
            try {
                String updateJson = "{\"status\":\"" + newStatus + "\"}";
                SupabaseApi.getInstance().patch("orders", "id=eq." + orderId, updateJson);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::loadOrders);
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private final List<Order> orders;
        private final OnOrderActionListener listener;

        interface OnOrderActionListener {
            void onStatusUpdate(String orderId, String newStatus);
        }

        OrderAdapter(List<Order> orders, OnOrderActionListener listener) {
            this.orders = orders;
            this.listener = listener;
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

            holder.nextStatusButton.setVisibility(View.VISIBLE);
            switch (order.getStatus()) {
                case "paid":
                    holder.nextStatusButton.setText("Start Preparing");
                    holder.nextStatusButton.setOnClickListener(v -> listener.onStatusUpdate(order.getId(), "preparing"));
                    break;
                case "preparing":
                    holder.nextStatusButton.setText("Mark Ready");
                    holder.nextStatusButton.setOnClickListener(v -> listener.onStatusUpdate(order.getId(), "ready"));
                    break;
                case "ready":
                    holder.nextStatusButton.setText("Mark Completed");
                    holder.nextStatusButton.setOnClickListener(v -> listener.onStatusUpdate(order.getId(), "completed"));
                    break;
                default:
                    holder.nextStatusButton.setVisibility(View.GONE);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate;
            Button nextStatusButton;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.order_id);
                orderStatus = itemView.findViewById(R.id.order_status);
                orderAmount = itemView.findViewById(R.id.order_amount);
                orderDate = itemView.findViewById(R.id.order_date);
                nextStatusButton = itemView.findViewById(R.id.next_status_button);
            }
        }
    }
}
