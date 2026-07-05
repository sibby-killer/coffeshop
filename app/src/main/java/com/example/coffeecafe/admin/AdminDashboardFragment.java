package com.example.coffeecafe.admin;

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
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.google.gson.Gson;

public class AdminDashboardFragment extends Fragment {
    private TextView totalShopsView, pendingAppsView, totalOrdersView, totalUsersView;
    private TextView welcomeText, noActivityText;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        totalShopsView = view.findViewById(R.id.total_shops);
        pendingAppsView = view.findViewById(R.id.pending_apps);
        totalOrdersView = view.findViewById(R.id.total_orders);
        totalUsersView = view.findViewById(R.id.total_users);
        welcomeText = view.findViewById(R.id.admin_welcome);
        noActivityText = view.findViewById(R.id.no_activity);
        progressBar = view.findViewById(R.id.progress_bar);

        String name = AuthManager.getInstance(getContext()).getCurrentProfile() != null
                ? AuthManager.getInstance(getContext()).getCurrentProfile().getFullName() : "Admin";
        welcomeText.setText("Welcome, " + name);

        // Click listeners for stat cards
        View cardUsers = view.findViewById(R.id.card_users);
        View cardShops = view.findViewById(R.id.card_shops);
        View cardOrders = view.findViewById(R.id.card_orders);

        if (cardUsers != null) cardUsers.setOnClickListener(v -> navigateTo(new com.example.coffeecafe.admin.ManageUsersFragment()));
        if (cardShops != null) cardShops.setOnClickListener(v -> navigateTo(new com.example.coffeecafe.admin.ManageShopsFragment()));
        if (cardOrders != null) cardOrders.setOnClickListener(v -> navigateTo(new com.example.coffeecafe.admin.ManageShopsFragment()));

        // Click listeners for quick action buttons
        View actionManageShops = view.findViewById(R.id.action_manage_shops);
        View actionManageUsers = view.findViewById(R.id.action_review_apps);

        if (actionManageShops != null) actionManageShops.setOnClickListener(v -> navigateTo(new com.example.coffeecafe.admin.ManageShopsFragment()));
        if (actionManageUsers != null) actionManageUsers.setOnClickListener(v -> navigateTo(new com.example.coffeecafe.admin.ManageUsersFragment()));

        loadDashboard();
    }

    private void navigateTo(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void loadDashboard() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                Gson gson = new Gson();

                String shopsResponse = SupabaseApi.getInstance().get("shops", "select=id", token);
                IdArray[] shops = gson.fromJson(shopsResponse, IdArray[].class);

                String ordersResponse = SupabaseApi.getInstance().get("orders", "select=id", token);
                IdArray[] orders = gson.fromJson(ordersResponse, IdArray[].class);

                String usersResponse = SupabaseApi.getInstance().get("profiles", "select=id", token);
                IdArray[] users = gson.fromJson(usersResponse, IdArray[].class);

                String productsResponse = SupabaseApi.getInstance().get("products", "select=id", token);
                IdArray[] products = gson.fromJson(productsResponse, IdArray[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        totalShopsView.setText(String.valueOf(shops != null ? shops.length : 0));
                        totalOrdersView.setText(String.valueOf(orders != null ? orders.length : 0));
                        totalUsersView.setText(String.valueOf(users != null ? users.length : 0));
                        pendingAppsView.setText(String.valueOf(products != null ? products.length : 0));
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        noActivityText.setText("Error loading data. Pull to refresh.");
                        noActivityText.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private static class IdArray {
        String id;
    }
}
