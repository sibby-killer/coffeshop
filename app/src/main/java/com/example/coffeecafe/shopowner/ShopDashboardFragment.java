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

import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

public class ShopDashboardFragment extends Fragment {
    private TextView totalOrdersView, pendingOrdersView, completedOrdersView, totalRevenueView;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        totalOrdersView = view.findViewById(R.id.total_orders);
        pendingOrdersView = view.findViewById(R.id.pending_orders);
        completedOrdersView = view.findViewById(R.id.completed_orders);
        totalRevenueView = view.findViewById(R.id.total_revenue);
        progressBar = view.findViewById(R.id.progress_bar);

        // Hide shop list since this is a dashboard view
        View shopListTitle = view.findViewById(R.id.title);
        View shopRecycler = view.findViewById(R.id.shops_recycler);
        if (shopListTitle != null) shopListTitle.setVisibility(View.GONE);
        if (shopRecycler != null) shopRecycler.setVisibility(View.GONE);

        // Show dashboard stat labels
        View dashboardStats = view.findViewById(R.id.dashboard_stats);
        if (dashboardStats != null) dashboardStats.setVisibility(View.VISIBLE);

        loadDashboard();
    }

    private void loadDashboard() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = SessionManager.getInstance(getContext()).getUserId();

        new Thread(() -> {
            try {
                String shopId = getShopId(userId);

                String query = "select=*&shop_id=eq." + shopId;
                String response = SupabaseApi.getInstance().get("orders", query);

                Gson gson = new Gson();
                Order[] orders = gson.fromJson(response, Order[].class);

                int total = 0;
                int pending = 0;
                int completed = 0;
                double revenue = 0;

                if (orders != null) {
                    total = orders.length;
                    for (Order o : orders) {
                        if ("pending".equals(o.status) || "paid".equals(o.status)) {
                            pending++;
                        }
                        if ("completed".equals(o.status)) {
                            completed++;
                        }
                        revenue += o.totalAmount;
                    }
                }

                int finalTotal = total;
                int finalPending = pending;
                int finalCompleted = completed;
                double finalRevenue = revenue;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        totalOrdersView.setText(String.valueOf(finalTotal));
                        pendingOrdersView.setText(String.valueOf(finalPending));
                        completedOrdersView.setText(String.valueOf(finalCompleted));
                        totalRevenueView.setText(String.format("KES %.0f", finalRevenue));
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
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

    // Minimal Order class for deserialization of stats data
    private static class Order {
        String status;
        double totalAmount;
    }
}
