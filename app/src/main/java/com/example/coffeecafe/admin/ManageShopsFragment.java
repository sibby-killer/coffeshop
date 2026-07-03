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
import com.example.coffeecafe.models.Shop;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ManageShopsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ShopAdapter adapter;
    private List<Shop> shopList;

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

        shopList = new ArrayList<>();
        adapter = new ShopAdapter(shopList, new ShopAdapter.OnShopActionListener() {
            @Override
            public void onToggleActive(Shop shop, boolean isActive) {
                toggleShopActive(shop.getId(), isActive);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadShops();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadShops();
    }

    private void loadShops() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("shops", query, token);

                Gson gson = new Gson();
                Shop[] shops = gson.fromJson(response, Shop[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (shops != null && shops.length > 0) {
                            shopList.clear();
                            for (Shop shop : shops) {
                                shopList.add(shop);
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

    private void toggleShopActive(String shopId, boolean isActive) {
        String token = AuthManager.getInstance(getContext()).getAccessToken();
        new Thread(() -> {
            try {
                String updateJson = "{\"is_active\":" + isActive + "}";
                SupabaseApi.getInstance().patch("shops", "id=eq." + shopId, updateJson, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Shop updated", Toast.LENGTH_SHORT).show();
                        loadShops();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }

    private class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {
        private final List<Shop> shops;
        private final OnShopActionListener listener;

        interface OnShopActionListener {
            void onToggleActive(Shop shop, boolean isActive);
        }

        ShopAdapter(List<Shop> shops, OnShopActionListener listener) {
            this.shops = shops;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new ShopViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
            Shop shop = shops.get(position);
            holder.orderId.setText(shop.getName());
            holder.orderStatus.setText(shop.isActive() ? "ACTIVE" : "INACTIVE");
            holder.orderAmount.setText(shop.getLocation());
            holder.orderDate.setText(shop.getCreatedAt());

            holder.nextStatusButton.setVisibility(View.VISIBLE);
            if (shop.isActive()) {
                holder.nextStatusButton.setText("Deactivate");
                holder.nextStatusButton.setOnClickListener(v -> listener.onToggleActive(shop, false));
            } else {
                holder.nextStatusButton.setText("Activate");
                holder.nextStatusButton.setOnClickListener(v -> listener.onToggleActive(shop, true));
            }
            holder.rejectButton.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return shops.size();
        }

        class ShopViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate;
            Button nextStatusButton, rejectButton;

            ShopViewHolder(@NonNull View itemView) {
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
