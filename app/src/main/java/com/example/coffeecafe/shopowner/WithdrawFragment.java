package com.example.coffeecafe.shopowner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.coffeecafe.models.Withdrawal;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class WithdrawFragment extends Fragment {
    private ProgressBar progressBar;
    private TextView balanceAmount, emptyHistory;
    private EditText fullNameInput, accountNumberInput, amountInput;
    private Spinner paymentMethodSpinner;
    private Button submitButton;
    private RecyclerView historyRecycler;
    private WithdrawalAdapter adapter;
    private List<Withdrawal> withdrawalList;
    private String shopId;
    private String userId;
    private double currentBalance = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_withdraw, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_bar);
        balanceAmount = view.findViewById(R.id.balance_amount);
        emptyHistory = view.findViewById(R.id.empty_history);
        fullNameInput = view.findViewById(R.id.full_name_input);
        accountNumberInput = view.findViewById(R.id.account_number_input);
        amountInput = view.findViewById(R.id.amount_input);
        paymentMethodSpinner = view.findViewById(R.id.payment_method_spinner);
        submitButton = view.findViewById(R.id.submit_withdrawal_button);
        historyRecycler = view.findViewById(R.id.history_recycler);

        withdrawalList = new ArrayList<>();
        adapter = new WithdrawalAdapter(withdrawalList);
        historyRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        historyRecycler.setAdapter(adapter);

        String[] paymentMethods = {"M-Pesa", "Airtel Money", "Bank"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, paymentMethods);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentMethodSpinner.setAdapter(spinnerAdapter);

        userId = SessionManager.getInstance(getContext()).getUserId();

        submitButton.setOnClickListener(v -> submitWithdrawal());

        loadShopId();
    }

    private void loadShopId() {
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
                        getActivity().runOnUiThread(() -> {
                            loadBalance();
                            loadWithdrawalHistory();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadBalance() {
        if (shopId == null) return;
        progressBar.setVisibility(View.VISIBLE);
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                // Get gross revenue from completed/paid orders
                String query = "select=total_amount&shop_id=eq." + shopId;
                String response = SupabaseApi.getInstance().get("orders", query, token);

                Gson gson = new Gson();
                Order[] orders = gson.fromJson(response, Order[].class);

                double revenue = 0;
                if (orders != null) {
                    for (Order order : orders) {
                        if ("completed".equals(order.getStatus()) || "paid".equals(order.getStatus())) {
                            revenue += order.getTotalAmount();
                        }
                    }
                }

                // Subtract approved/completed withdrawals
                String withdrawalQuery = "select=amount&shop_owner_id=eq." + userId + "&status=in.(approved,completed)";
                String withdrawalResponse = SupabaseApi.getInstance().get("withdrawals", withdrawalQuery, token);

                Withdrawal[] withdrawals = gson.fromJson(withdrawalResponse, Withdrawal[].class);
                double withdrawn = 0;
                if (withdrawals != null) {
                    for (Withdrawal w : withdrawals) {
                        withdrawn += w.getAmount();
                    }
                }

                currentBalance = revenue - withdrawn;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        balanceAmount.setText(String.format("KES %.0f", currentBalance));
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }
        }).start();
    }

    private void loadWithdrawalHistory() {
        if (userId == null) return;
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*&shop_owner_id=eq." + userId + "&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("withdrawals", query, token);

                Gson gson = new Gson();
                Withdrawal[] withdrawals = gson.fromJson(response, Withdrawal[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        withdrawalList.clear();
                        if (withdrawals != null && withdrawals.length > 0) {
                            for (Withdrawal w : withdrawals) {
                                withdrawalList.add(w);
                            }
                            adapter.notifyDataSetChanged();
                            emptyHistory.setVisibility(View.GONE);
                        } else {
                            emptyHistory.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> emptyHistory.setVisibility(View.VISIBLE));
                }
            }
        }).start();
    }

    private void submitWithdrawal() {
        String fullName = fullNameInput.getText().toString().trim();
        String accountNumber = accountNumberInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String paymentMethod = paymentMethodSpinner.getSelectedItem().toString();

        if (fullName.isEmpty() || accountNumber.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            Toast.makeText(getContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount > currentBalance) {
            Toast.makeText(getContext(), "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        new Thread(() -> {
            try {
                Withdrawal withdrawal = new Withdrawal(userId, fullName, paymentMethod, accountNumber, amount);
                String json = new Gson().toJson(withdrawal);
                String token = AuthManager.getInstance(getContext()).getAccessToken();

                SupabaseApi.getInstance().post("withdrawals", json, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        fullNameInput.setText("");
                        accountNumberInput.setText("");
                        amountInput.setText("");
                        Toast.makeText(getContext(), "Withdrawal request submitted!", Toast.LENGTH_SHORT).show();
                        loadBalance();
                        loadWithdrawalHistory();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private static class ShopId {
        String id;
    }

    private class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalAdapter.WithdrawalViewHolder> {
        private final List<Withdrawal> withdrawals;

        WithdrawalAdapter(List<Withdrawal> withdrawals) {
            this.withdrawals = withdrawals;
        }

        @NonNull
        @Override
        public WithdrawalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale_order, parent, false);
            return new WithdrawalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WithdrawalViewHolder holder, int position) {
            Withdrawal withdrawal = withdrawals.get(position);
            String displayId = withdrawal.getId() != null && withdrawal.getId().length() >= 8
                    ? withdrawal.getId().substring(0, 8) : "N/A";
            holder.orderId.setText("Withdrawal #" + displayId);
            holder.orderStatus.setText(withdrawal.getStatusDisplay());
            holder.orderAmount.setText(String.format("KES %.0f", withdrawal.getAmount()));
            holder.orderDate.setText(withdrawal.getCreatedAt() != null ? withdrawal.getCreatedAt().substring(0, 10) : "");
            holder.orderCustomer.setText("Name: " + withdrawal.getFullName());
            holder.orderItems.setText("Method: " + withdrawal.getPaymentMethod() + " | Acc: " + withdrawal.getAccountNumber());

            int bgColor;
            switch (withdrawal.getStatus()) {
                case "approved":
                case "completed":
                    bgColor = R.color.status_completed;
                    break;
                case "pending":
                    bgColor = R.color.status_pending;
                    break;
                case "rejected":
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
            return withdrawals.size();
        }

        class WithdrawalViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderStatus, orderAmount, orderDate, orderCustomer, orderItems;

            WithdrawalViewHolder(@NonNull View itemView) {
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
