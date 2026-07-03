package com.example.coffeecafe.admin;

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
import com.example.coffeecafe.models.ShopApplication;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ApplicationsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ApplicationAdapter adapter;
    private List<ShopApplication> applicationList;

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

        applicationList = new ArrayList<>();
        adapter = new ApplicationAdapter(applicationList, new ApplicationAdapter.OnApplicationActionListener() {
            @Override
            public void onApprove(ShopApplication app) {
                updateApplicationStatus(app.getId(), "approved");
            }

            @Override
            public void onReject(ShopApplication app) {
                updateApplicationStatus(app.getId(), "rejected");
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadApplications();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadApplications();
    }

    private void loadApplications() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*,profiles(full_name)&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("shop_applications", query, token);

                Gson gson = new Gson();
                ShopApplication[] apps = gson.fromJson(response, ShopApplication[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (apps != null && apps.length > 0) {
                            applicationList.clear();
                            for (ShopApplication app : apps) {
                                applicationList.add(app);
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
                        emptyView.setText("Failed to load: " + e.getMessage());
                        emptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private void updateApplicationStatus(String appId, String status) {
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String updateJson = "{\"status\":\"" + status + "\"}";
                SupabaseApi.getInstance().patch("shop_applications", "id=eq." + appId, updateJson, token);

                if (status.equals("approved")) {
                    approveShop(appId);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Application " + status, Toast.LENGTH_SHORT).show();
                        loadApplications();
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
    }

    private void approveShop(String applicationId) {
        try {
            String token = AuthManager.getInstance(getContext()).getAccessToken();
            String query = "select=*&id=eq." + applicationId + "&limit=1";
            String response = SupabaseApi.getInstance().get("shop_applications", query, token);

            Gson gson = new Gson();
            ShopApplication[] apps = gson.fromJson(response, ShopApplication[].class);
            if (apps != null && apps.length > 0) {
                ShopApplication app = apps[0];

                String shopJson = "{\"application_id\":\"" + escapeJson(app.getId()) +
                        "\",\"owner_id\":\"" + escapeJson(app.getOwnerId()) +
                        "\",\"name\":\"" + escapeJson(app.getShopName()) +
                        "\",\"description\":\"" + escapeJson(app.getShopDescription()) +
                        "\",\"location\":\"" + escapeJson(app.getLocation()) +
                        "\",\"phone\":\"" + escapeJson(app.getPhone()) + "\"}";

                SupabaseApi.getInstance().post("shops", shopJson, token);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder> {
        private final List<ShopApplication> applications;
        private final OnApplicationActionListener listener;

        interface OnApplicationActionListener {
            void onApprove(ShopApplication app);
            void onReject(ShopApplication app);
        }

        ApplicationAdapter(List<ShopApplication> applications, OnApplicationActionListener listener) {
            this.applications = applications;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new ApplicationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
            ShopApplication app = applications.get(position);
            holder.orderId.setText(app.getShopName());
            holder.orderStatus.setText(app.getStatus().toUpperCase());
            holder.orderAmount.setText(app.getLocation());
            holder.orderDate.setText(app.getCreatedAt());

            holder.nextStatusButton.setVisibility(View.VISIBLE);
            if ("pending".equals(app.getStatus())) {
                holder.nextStatusButton.setText("Approve");
                holder.nextStatusButton.setOnClickListener(v -> listener.onApprove(app));
                holder.rejectButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setOnClickListener(v -> listener.onReject(app));
            } else {
                holder.nextStatusButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return applications.size();
        }

        class ApplicationViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate;
            Button nextStatusButton, rejectButton;

            ApplicationViewHolder(@NonNull View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.order_id);
                orderStatus = itemView.findViewById(R.id.order_status);
                orderAmount = itemView.findViewById(R.id.order_amount);
                orderDate = itemView.findViewById(R.id.order_date);
                nextStatusButton = itemView.findViewById(R.id.next_status_button);
                rejectButton = itemView.findViewById(R.id.reject_button);
            }
        }
    }
}
