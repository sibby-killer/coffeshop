package com.example.coffeecafe.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
import com.example.coffeecafe.utils.CartManager;

import java.util.List;

public class CartFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyView, totalView;
    private Button checkoutButton;
    private CartAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.cart_recycler);
        emptyView = view.findViewById(R.id.empty_view);
        totalView = view.findViewById(R.id.total_view);
        checkoutButton = view.findViewById(R.id.checkout_button);

        adapter = new CartAdapter(CartManager.getInstance(getContext()).getCartItems(), new CartAdapter.OnCartActionListener() {
            @Override
            public void onQuantityChanged() {
                updateTotal();
            }

            @Override
            public void onRemoveItem(String productId) {
                CartManager.getInstance(getContext()).removeFromCart(productId);
                adapter.updateItems(CartManager.getInstance(getContext()).getCartItems());
                updateTotal();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        checkoutButton.setOnClickListener(v -> {
            if (CartManager.getInstance(getContext()).getCartItems().isEmpty()) {
                Toast.makeText(getContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(getContext(), CheckoutActivity.class));
        });

        updateTotal();
    }

    private void updateTotal() {
        List<CartManager.CartItem> items = CartManager.getInstance(getContext()).getCartItems();
        if (items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            checkoutButton.setEnabled(false);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            checkoutButton.setEnabled(true);
        }
        double total = CartManager.getInstance(getContext()).getCartTotal();
        totalView.setText(String.format("Total: $%.2f", total));
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
        private List<CartManager.CartItem> items;
        private final OnCartActionListener listener;

        interface OnCartActionListener {
            void onQuantityChanged();
            void onRemoveItem(String productId);
        }

        CartAdapter(List<CartManager.CartItem> items, OnCartActionListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void updateItems(List<CartManager.CartItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
            return new CartViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
            CartManager.CartItem item = items.get(position);
            holder.itemName.setText(item.getProductName());
            holder.itemPrice.setText(String.format("$%.2f", item.getPrice()));
            holder.itemQuantity.setText(String.valueOf(item.getQuantity()));
            holder.itemSubtotal.setText(String.format("$%.2f", item.getSubtotal()));

            holder.minusButton.setOnClickListener(v -> {
                CartManager.getInstance(getContext()).updateQuantity(item.getProductId(), item.getQuantity() - 1);
                updateItems(CartManager.getInstance(getContext()).getCartItems());
                listener.onQuantityChanged();
            });

            holder.plusButton.setOnClickListener(v -> {
                CartManager.getInstance(getContext()).updateQuantity(item.getProductId(), item.getQuantity() + 1);
                updateItems(CartManager.getInstance(getContext()).getCartItems());
                listener.onQuantityChanged();
            });

            holder.removeButton.setOnClickListener(v -> listener.onRemoveItem(item.getProductId()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class CartViewHolder extends RecyclerView.ViewHolder {
            TextView itemName, itemPrice, itemQuantity, itemSubtotal;
            Button minusButton, plusButton, removeButton;

            CartViewHolder(@NonNull View itemView) {
                super(itemView);
                itemName = itemView.findViewById(R.id.item_name);
                itemPrice = itemView.findViewById(R.id.item_price);
                itemQuantity = itemView.findViewById(R.id.item_quantity);
                itemSubtotal = itemView.findViewById(R.id.item_subtotal);
                minusButton = itemView.findViewById(R.id.minus_button);
                plusButton = itemView.findViewById(R.id.plus_button);
                removeButton = itemView.findViewById(R.id.remove_button);
            }
        }
    }
}
