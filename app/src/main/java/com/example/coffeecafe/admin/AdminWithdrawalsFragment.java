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
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminWithdrawalsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private WithdrawalAdapter adapter;
    private List<WithdrawalRequest> withdrawalList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_withdrawals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.withdrawals_recycler);
        progressBar = view.findViewById(R.id.progress_bar);

        withdrawalList = new ArrayList<>();
        adapter = new WithdrawalAdapter(withdrawalList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadWithdrawals();
    }

    private void loadWithdrawals() {
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("withdrawals", query, token);

                Gson gson = new Gson();
                WithdrawalRequest[] withdrawals = gson.fromJson(response, WithdrawalRequest[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        withdrawalList.clear();
                        if (withdrawals != null) {
                            for (WithdrawalRequest w : withdrawals) withdrawalList.add(w);
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

    private void updateStatus(String withdrawalId, String status) {
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("status", status);
                String json = new Gson().toJson(data);

                SupabaseApi.getInstance().patch("withdrawals", "id=eq." + withdrawalId, json, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Withdrawal " + status, Toast.LENGTH_SHORT).show();
                        loadWithdrawals();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    public static class WithdrawalRequest {
        @SerializedName("id") public String id;
        @SerializedName("shop_owner_id") public String shopOwnerId;
        @SerializedName("full_name") public String fullName;
        @SerializedName("payment_method") public String paymentMethod;
        @SerializedName("account_number") public String accountNumber;
        @SerializedName("amount") public double amount;
        @SerializedName("status") public String status;
        @SerializedName("created_at") public String createdAt;
    }

    private class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalAdapter.WithdrawalViewHolder> {
        private final List<WithdrawalRequest> withdrawals;
        WithdrawalAdapter(List<WithdrawalRequest> withdrawals) { this.withdrawals = withdrawals; }

        @NonNull
        @Override
        public WithdrawalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_withdrawal, parent, false);
            return new WithdrawalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WithdrawalViewHolder holder, int position) {
            WithdrawalRequest w = withdrawals.get(position);
            holder.fullName.setText(w.fullName);
            holder.paymentMethod.setText(w.paymentMethod + " - " + w.accountNumber);
            holder.amount.setText(String.format("KES %.0f", w.amount));
            holder.status.setText(w.status);
            holder.date.setText(w.createdAt != null ? w.createdAt.substring(0, 10) : "");

            boolean isPending = "pending".equals(w.status);
            holder.approveButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
            holder.rejectButton.setVisibility(isPending ? View.VISIBLE : View.GONE);

            holder.approveButton.setOnClickListener(v -> updateStatus(w.id, "approved"));
            holder.rejectButton.setOnClickListener(v -> updateStatus(w.id, "rejected"));
        }

        @Override
        public int getItemCount() { return withdrawals.size(); }

        class WithdrawalViewHolder extends RecyclerView.ViewHolder {
            TextView fullName, paymentMethod, amount, status, date;
            Button approveButton, rejectButton;
            WithdrawalViewHolder(@NonNull View itemView) {
                super(itemView);
                fullName = itemView.findViewById(R.id.full_name);
                paymentMethod = itemView.findViewById(R.id.payment_method);
                amount = itemView.findViewById(R.id.amount);
                status = itemView.findViewById(R.id.status);
                date = itemView.findViewById(R.id.date);
                approveButton = itemView.findViewById(R.id.approve_button);
                rejectButton = itemView.findViewById(R.id.reject_button);
            }
        }
    }
}
