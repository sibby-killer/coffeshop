package com.example.coffeecafe.shopowner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ProductsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText productNameInput, productDescInput, productPriceInput, productCategoryInput;
    private Button addProductButton;
    private ProductAdapter adapter;
    private List<Product> productList;
    private String shopId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drinks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.products_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        productNameInput = view.findViewById(R.id.product_name_input);
        productDescInput = view.findViewById(R.id.product_desc_input);
        productPriceInput = view.findViewById(R.id.product_price_input);
        productCategoryInput = view.findViewById(R.id.product_category_input);
        addProductButton = view.findViewById(R.id.add_product_button);

        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        addProductButton.setOnClickListener(v -> addProduct());

        loadShopId();
    }

    private void loadShopId() {
        String userId = SessionManager.getInstance(getContext()).getUserId();
        new Thread(() -> {
            try {
                String query = "select=id&owner_id=eq." + userId + "&limit=1";
                String response = SupabaseApi.getInstance().get("shops", query);

                Gson gson = new Gson();
                ShopId[] shops = gson.fromJson(response, ShopId[].class);
                if (shops != null && shops.length > 0) {
                    shopId = shops[0].id;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadProducts);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadProducts() {
        if (shopId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String query = "select=*&shop_id=eq." + shopId + "&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("products", query);

                Gson gson = new Gson();
                Product[] products = gson.fromJson(response, Product[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        productList.clear();
                        if (products != null) {
                            for (Product p : products) {
                                productList.add(p);
                            }
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

    private void addProduct() {
        String name = productNameInput.getText().toString().trim();
        String desc = productDescInput.getText().toString().trim();
        String priceStr = productPriceInput.getText().toString().trim();
        String category = productCategoryInput.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(getContext(), "Name and price required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);

        progressBar.setVisibility(View.VISIBLE);
        addProductButton.setEnabled(false);

        new Thread(() -> {
            try {
                Product product = new Product(name, desc, price, category.isEmpty() ? "general" : category);
                product.setShopId(shopId);

                String json = new Gson().toJson(product);

                SupabaseApi.getInstance().post("products", json, null);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addProductButton.setEnabled(true);
                        productNameInput.setText("");
                        productDescInput.setText("");
                        productPriceInput.setText("");
                        productCategoryInput.setText("");
                        Toast.makeText(getContext(), "Product added!", Toast.LENGTH_SHORT).show();
                        loadProducts();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addProductButton.setEnabled(true);
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }

    private static class ShopId {
        String id;
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final List<Product> products;

        ProductAdapter(List<Product> products) {
            this.products = products;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.name.setText(product.getName());
            holder.price.setText(String.format("$%.2f", product.getPrice()));
            holder.description.setText(product.getDescription());
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView name, price, description;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.shop_name);
                price = itemView.findViewById(R.id.shop_location);
                description = itemView.findViewById(R.id.shop_description);
            }
        }
    }
}
