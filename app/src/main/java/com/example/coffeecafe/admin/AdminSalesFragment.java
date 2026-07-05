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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class AdminSalesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView totalRevenueText, totalOrdersText, totalShopsText;
    private SaleAdapter adapter;
    private List<SaleOrder> saleList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_sales, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.sales_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        totalRevenueText = view.findViewById(R.id.total_revenue);
        totalOrdersText = view.findViewById(R.id.total_orders);

        saleList = new ArrayList<>();
        adapter = new SaleAdapter(saleList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadSales();
    }

    private void loadSales() {
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*,shops(name)&order=created_at.desc&limit=100";
                String response = SupabaseApi.getInstance().get("orders", query, token);

                Gson gson = new Gson();
                SaleOrder[] orders = gson.fromJson(response, SaleOrder[].class);

                double totalRevenue = 0;
                int totalOrders = 0;

                if (orders != null) {
                    for (SaleOrder order : orders) {
                        totalRevenue += order.totalAmount;
                        totalOrders++;
                    }
                }

                final double finalRevenue = totalRevenue;
                final int finalTotalOrders = totalOrders;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        totalRevenueText.setText(String.format("KES %.0f", finalRevenue));
                        totalOrdersText.setText(String.valueOf(finalTotalOrders));

                        saleList.clear();
                        if (orders != null) {
                            for (SaleOrder o : orders) saleList.add(o);
                        }
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }
        }).start();
    }

    public static class SaleOrder {
        @SerializedName("id") public String id;
        @SerializedName("total_amount") public double totalAmount;
        @SerializedName("status") public String status;
        @SerializedName("created_at") public String createdAt;
        @SerializedName("payment_method") public String paymentMethod;
        @SerializedName("shops") public ShopName shopName;

        public String getShopName() {
            return shopName != null ? shopName.name : "Unknown";
        }
    }

    public static class ShopName {
        @SerializedName("name") public String name;
    }

    private class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.SaleViewHolder> {
        private final List<SaleOrder> orders;
        SaleAdapter(List<SaleOrder> orders) { this.orders = orders; }

        @NonNull
        @Override
        public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_sale, parent, false);
            return new SaleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
            SaleOrder order = orders.get(position);
            holder.orderId.setText("Order #" + order.id.substring(0, 8));
            holder.shopName.setText(order.getShopName());
            holder.amount.setText(String.format("KES %.0f", order.totalAmount));
            holder.status.setText(order.status);
            holder.date.setText(order.createdAt != null ? order.createdAt.substring(0, 10) : "");
        }

        @Override
        public int getItemCount() { return orders.size(); }

        class SaleViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, shopName, amount, status, date;
            SaleViewHolder(@NonNull View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.order_id);
                shopName = itemView.findViewById(R.id.shop_name);
                amount = itemView.findViewById(R.id.amount);
                status = itemView.findViewById(R.id.status);
                date = itemView.findViewById(R.id.date);
            }
        }
    }
}
