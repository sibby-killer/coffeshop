package com.example.coffeecafe.customer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coffeecafe.R;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.models.Shop;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CustomerHomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchInput;
    private Spinner categoryFilter;
    private ShopAdapter adapter;
    private List<Shop> shopList;
    private List<Shop> filteredList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.shops_recycler);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        searchInput = view.findViewById(R.id.search_input);
        categoryFilter = view.findViewById(R.id.category_filter);

        shopList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new ShopAdapter(filteredList, shop -> {
            Bundle args = new Bundle();
            args.putString("shop_id", shop.getId());
            args.putString("shop_name", shop.getName());
            ShopDetailFragment detailFragment = new ShopDetailFragment();
            detailFragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Category filter
        String[] categories = {"All", "Coffee", "Drinks", "Snacks", "Food"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilter.setAdapter(catAdapter);
        categoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterShops();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterShops();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadShops();
    }

    private void loadShops() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String query = "select=*&is_active=eq.true&order=created_at.desc";
                String response = SupabaseApi.getInstance().get("shops", query);

                Gson gson = new Gson();
                Shop[] shops = gson.fromJson(response, Shop[].class);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        shopList.clear();
                        if (shops != null && shops.length > 0) {
                            for (Shop shop : shops) {
                                shopList.add(shop);
                            }
                        }
                        filterShops();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setText("Could not load shops. Please try again.");
                        emptyView.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }

    private void filterShops() {
        String search = searchInput.getText().toString().trim().toLowerCase();
        String category = categoryFilter.getSelectedItem() != null
                ? categoryFilter.getSelectedItem().toString() : "All";

        filteredList.clear();
        for (Shop shop : shopList) {
            boolean matchesSearch = search.isEmpty()
                    || shop.getName().toLowerCase().contains(search)
                    || shop.getDescription().toLowerCase().contains(search)
                    || shop.getLocation().toLowerCase().contains(search);

            boolean matchesCategory = category.equals("All")
                    || shop.getDescription().toLowerCase().contains(category.toLowerCase());

            if (matchesSearch && matchesCategory) {
                filteredList.add(shop);
            }
        }

        adapter.notifyDataSetChanged();
        emptyView.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredList.isEmpty()) {
            emptyView.setText("No shops found matching your search");
        }
    }

    private class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {
        private final List<Shop> shops;
        private final OnShopClickListener listener;

        interface OnShopClickListener {
            void onShopClick(Shop shop);
        }

        ShopAdapter(List<Shop> shops, OnShopClickListener listener) {
            this.shops = shops;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            return new ShopViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
            Shop shop = shops.get(position);
            holder.shopName.setText(shop.getName());
            holder.shopLocation.setText(shop.getLocation());
            holder.shopDescription.setText(shop.getDescription());

            // Load shop logo if available
            if (shop.getImageUrl() != null && !shop.getImageUrl().isEmpty()) {
                loadShopImage(shop.getImageUrl(), holder.shopLogo);
            } else {
                holder.shopLogo.setImageResource(R.drawable.ic_shop);
            }

            holder.itemView.setOnClickListener(v -> listener.onShopClick(shop));
        }

        @Override
        public int getItemCount() {
            return shops.size();
        }

        class ShopViewHolder extends RecyclerView.ViewHolder {
            TextView shopName, shopLocation, shopDescription;
            ImageView shopLogo;

            ShopViewHolder(@NonNull View itemView) {
                super(itemView);
                shopName = itemView.findViewById(R.id.shop_name);
                shopLocation = itemView.findViewById(R.id.shop_location);
                shopDescription = itemView.findViewById(R.id.shop_description);
                shopLogo = itemView.findViewById(R.id.shop_logo);
            }
        }
    }

    private void loadShopImage(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            try {
                InputStream in = new URL(imageUrl).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                // Keep default icon on failure
            }
        }).start();
    }
}
