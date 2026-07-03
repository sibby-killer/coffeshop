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

        loadDashboard();
    }

    private void loadDashboard() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                Gson gson = new Gson();

                String shopsResponse = SupabaseApi.getInstance().get("shops", "select=id", token);
                IdArray[] shops = gson.fromJson(shopsResponse, IdArray[].class);

                String appsResponse = SupabaseApi.getInstance().get("shop_applications", "select=id&status=eq.pending", token);
                IdArray[] apps = gson.fromJson(appsResponse, IdArray[].class);

                String ordersResponse = SupabaseApi.getInstance().get("orders", "select=id", token);
                IdArray[] orders = gson.fromJson(ordersResponse, IdArray[].class);

                String usersResponse = SupabaseApi.getInstance().get("profiles", "select=id", token);
                IdArray[] users = gson.fromJson(usersResponse, IdArray[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        totalShopsView.setText(String.valueOf(shops != null ? shops.length : 0));
                        pendingAppsView.setText(String.valueOf(apps != null ? apps.length : 0));
                        totalOrdersView.setText(String.valueOf(orders != null ? orders.length : 0));
                        totalUsersView.setText(String.valueOf(users != null ? users.length : 0));
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
