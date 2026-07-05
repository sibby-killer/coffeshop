package com.example.coffeecafe.shopowner;

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
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Order;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ShopSalesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView, totalRevenue, totalOrders;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private String shopId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop_sales, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.orders_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        totalRevenue = view.findViewById(R.id.total_revenue);
        totalOrders = view.findViewById(R.id.total_orders);

        orderList = new ArrayList<>();
        adapter = new OrderAdapter(orderList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadShopId();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shopId != null) {
            loadSales();
        }
    }

    private void loadShopId() {
        String userId = SessionManager.getInstance(getContext()).getUserId();
        String token = AuthManager.getInstance(getContext()).getAccessToken();
        new Thread(() -> {
            try {
                String query = "select=id&owner_id=eq." + userId + "&limit=1";
                String response = SupabaseApi.getInstance().get("shops", query, token);

                Gson gson = new Gson();
                ShopId[] shops = gson.fromJson(response, ShopId[].class);
                if (shops != null && shops.length > 0) {
                    shopId = shops[0].id;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadSales);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSales() {
        if (shopId == null) return;
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*&shop_id=eq." + shopId + "&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("orders", query, token);

                Gson gson = new Gson();
                Order[] orders = gson.fromJson(response, Order[].class);

                double revenue = 0;
                int count = 0;
                List<Order> completedOrders = new ArrayList<>();

                if (orders != null) {
                    for (Order order : orders) {
                        if ("completed".equals(order.getStatus()) || "paid".equals(order.getStatus())) {
                            revenue += order.getTotalAmount();
                        }
                        count++;
                        completedOrders.add(order);
                    }
                }

                double finalRevenue = revenue;
                int finalCount = count;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        totalRevenue.setText(String.format("KES %.0f", finalRevenue));
                        totalOrders.setText(String.valueOf(finalCount));

                        orderList.clear();
                        if (completedOrders.size() > 0) {
                            orderList.addAll(completedOrders);
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
                        emptyView.setText("Failed to load sales: " + e.getMessage());
                        emptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private static class ShopId {
        String id;
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private final List<Order> orders;

        OrderAdapter(List<Order> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale_order, parent, false);
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
            holder.orderDate.setText(order.getCreatedAt() != null ? order.getCreatedAt().substring(0, 10) : "");
            holder.orderCustomer.setText("Customer: " + (order.getCustomerId() != null ? order.getCustomerId().substring(0, Math.min(8, order.getCustomerId().length())) : "N/A"));
            holder.orderItems.setText("Payment: " + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A"));

            int bgColor;
            switch (order.getStatus()) {
                case "completed":
                    bgColor = R.color.status_completed;
                    break;
                case "paid":
                    bgColor = R.color.status_paid;
                    break;
                case "pending":
                    bgColor = R.color.status_pending;
                    break;
                case "cancelled":
                    bgColor = R.color.status_cancelled;
                    break;
                default:
                    bgColor = R.color.primary;
                    break;
            }
            holder.orderStatus.setBackgroundColor(holder.itemView.getContext().getResources().getColor(bgColor));
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate, orderCustomer, orderItems;

            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.order_id);
                orderStatus = itemView.findViewById(R.id.order_status);
                orderAmount = itemView.findViewById(R.id.order_amount);
                orderDate = itemView.findViewById(R.id.order_date);
                orderCustomer = itemView.findViewById(R.id.order_customer);
                orderItems = itemView.findViewById(R.id.order_items);
            }
        }
    }
}
