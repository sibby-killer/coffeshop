package com.example.coffeecafe.customer;

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
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Product;
import com.example.coffeecafe.utils.CartManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ShopDetailFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView, shopNameView;
    private Button cartButton;
    private ProductAdapter adapter;
    private List<Product> productList;
    private String shopId, shopName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coffee_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            shopId = args.getString("shop_id");
            shopName = args.getString("shop_name");
        }

        recyclerView = view.findViewById(R.id.products_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        shopNameView = view.findViewById(R.id.shop_name);
        cartButton = view.findViewById(R.id.cart_button);

        if (shopNameView != null) shopNameView.setText(shopName);

        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList, product -> {
            CartManager.getInstance(getContext()).addToCart(
                    new CartManager.CartItem(
                            product.getId(),
                            product.getShopId(),
                            product.getName(),
                            product.getPrice(),
                            product.getImageUrl()
                    )
            );
            Toast.makeText(getContext(), product.getName() + " added to cart", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (cartButton != null) {
            cartButton.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CartFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        loadProducts();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String query = "select=*&shop_id=eq." + shopId + "&is_available=eq.true&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("products", query);

                Gson gson = new Gson();
                Product[] products = gson.fromJson(response, Product[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (products != null && products.length > 0) {
                            productList.clear();
                            for (Product product : products) {
                                productList.add(product);
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
                        emptyView.setText("Failed to load products: " + e.getMessage());
                        emptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final List<Product> products;
        private final OnProductClickListener listener;

        interface OnProductClickListener {
            void onAddToCart(Product product);
        }

        ProductAdapter(List<Product> products, OnProductClickListener listener) {
            this.products = products;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.productName.setText(product.getName());
            holder.productPrice.setText(String.format("KES %.0f", product.getPrice()));
            holder.productDescription.setText(product.getDescription());
            holder.addToCartButton.setOnClickListener(v -> listener.onAddToCart(product));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView productName, productPrice, productDescription;
            Button addToCartButton;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.product_name);
                productPrice = itemView.findViewById(R.id.product_price);
                productDescription = itemView.findViewById(R.id.product_description);
                addToCartButton = itemView.findViewById(R.id.add_to_cart_button);
            }
        }
    }
}
