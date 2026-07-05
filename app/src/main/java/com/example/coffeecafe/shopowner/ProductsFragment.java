package com.example.coffeecafe.shopowner;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Product;
import com.example.coffeecafe.utils.SessionManager;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProductsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private EditText productNameInput, productDescInput, productPriceInput, productCategoryInput, productQuantityInput;
    private Button addProductButton, pickImageButton;
    private ImageView productImagePreview;
    private TextView imageStatusText;
    private ProductAdapter adapter;
    private List<Product> productList;
    private String shopId;
    private Uri selectedImageUri;
    private String uploadedImageUrl;

    private static final int PICK_IMAGE_REQUEST = 1001;

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
        productQuantityInput = view.findViewById(R.id.product_quantity_input);
        addProductButton = view.findViewById(R.id.add_product_button);
        pickImageButton = view.findViewById(R.id.pick_image_button);
        productImagePreview = view.findViewById(R.id.product_image_preview);
        imageStatusText = view.findViewById(R.id.image_status_text);

        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        addProductButton.setOnClickListener(v -> addProduct());
        pickImageButton.setOnClickListener(v -> openImagePicker());

        loadShopId();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Product Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            productImagePreview.setImageURI(selectedImageUri);
            imageStatusText.setText("Image selected ✓");
            imageStatusText.setTextColor(0xFF4CAF50);
        }
    }

    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int colIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(colIndex);
            cursor.close();
            return path;
        }
        return uri.getPath();
    }

    private void loadShopId() {
        String userId = SessionManager.getInstance(getContext()).getUserId();
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
        String token = AuthManager.getInstance(getContext()).getAccessToken();

        new Thread(() -> {
            try {
                String query = "select=*&shop_id=eq." + shopId + "&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("products", query, token);

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
        String quantityStr = productQuantityInput.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(getContext(), "Name and price required", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int quantity = quantityStr.isEmpty() ? 0 : Integer.parseInt(quantityStr);

        progressBar.setVisibility(View.VISIBLE);
        addProductButton.setEnabled(false);

        new Thread(() -> {
            try {
                // Upload image if selected
                String imageUrl = "";
                if (selectedImageUri != null) {
                    String filePath = getRealPathFromUri(selectedImageUri);
                    if (filePath != null) {
                        String token = AuthManager.getInstance(getContext()).getAccessToken();
                        imageUrl = SupabaseApi.getInstance().uploadFile("products", filePath, true, token);
                    }
                }

                Product product = new Product(name, desc, price, category.isEmpty() ? "general" : category, quantity);
                product.setShopId(shopId);
                product.setImageUrl(imageUrl);

                String json = new Gson().toJson(product);
                String token = AuthManager.getInstance(getContext()).getAccessToken();

                SupabaseApi.getInstance().post("products", json, token);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addProductButton.setEnabled(true);
                        productNameInput.setText("");
                        productDescInput.setText("");
                        productPriceInput.setText("");
                        productCategoryInput.setText("");
                        productQuantityInput.setText("");
                        productImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
                        imageStatusText.setText("No image selected");
                        imageStatusText.setTextColor(0xFF9E9E9E);
                        selectedImageUri = null;
                        uploadedImageUrl = null;
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

    private void deleteProduct(String productId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    String token = AuthManager.getInstance(getContext()).getAccessToken();
                    new Thread(() -> {
                        try {
                            SupabaseApi.getInstance().delete("products", "id=eq." + productId, token);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                                    loadProducts();
                                });
                            }
                        } catch (Exception e) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_manage, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.name.setText(product.getName());
            holder.price.setText(String.format("KES %.0f", product.getPrice()));
            holder.quantity.setText("Stock: " + product.getQuantity());
            holder.description.setText(product.getDescription());
            holder.btnDelete.setOnClickListener(v -> deleteProduct(product.getId()));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView name, price, quantity, description;
            ImageButton btnDelete;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.product_name);
                price = itemView.findViewById(R.id.product_price);
                quantity = itemView.findViewById(R.id.product_quantity);
                description = itemView.findViewById(R.id.product_description);
                btnDelete = itemView.findViewById(R.id.btn_delete_product);
            }
        }
    }
}
