package com.example.coffeecafe;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.coffeecafe.admin.AdminDashboardFragment;
import com.example.coffeecafe.admin.AdminSalesFragment;
import com.example.coffeecafe.admin.AdminWithdrawalsFragment;
import com.example.coffeecafe.admin.ManageShopsFragment;
import com.example.coffeecafe.admin.ManageUsersFragment;
import com.example.coffeecafe.auth.AuthManager;
import com.example.coffeecafe.customer.CartFragment;
import com.example.coffeecafe.customer.CustomerHomeFragment;
import com.example.coffeecafe.customer.CustomerOrdersFragment;
import com.example.coffeecafe.profile.ProfileFragment;
import com.example.coffeecafe.shopowner.MyShopFragment;
import com.example.coffeecafe.shopowner.ProductsFragment;
import com.example.coffeecafe.shopowner.ShopDashboardFragment;
import com.example.coffeecafe.shopowner.ShopOrdersFragment;
import com.example.coffeecafe.shopowner.ShopSalesFragment;
import com.example.coffeecafe.shopowner.WithdrawFragment;
import com.example.coffeecafe.config.SupabaseApi;
import com.example.coffeecafe.utils.CartManager;
import com.example.coffeecafe.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

public class DashBoard extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private String userRole;
    private TextView cartBadgeTextView;
    private TextView orderBadgeTextView;
    private CartManager.CartUpdateListener cartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_dash_board);

        userRole = getIntent().getStringExtra("role");
        if (userRole == null) {
            userRole = AuthManager.getInstance(this).getCurrentRole();
        }

        bottomNavigationView = findViewById(R.id.bottom_nav_bar);

        setupNavigationForRole();
    }

    private void setupNavigationForRole() {
        Menu menu = bottomNavigationView.getMenu();
        menu.clear();

        if (userRole == null) {
            userRole = "customer";
        }

        switch (userRole) {
            case "admin":
                setupAdminNavigation(menu);
                break;
            case "shop_owner":
                setupShopOwnerNavigation(menu);
                break;
            default:
                setupCustomerNavigation(menu);
                break;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (userRole.equals("admin")) {
                if (itemId == R.id.nav_dashboard) {
                    fragment = new AdminDashboardFragment();
                } else if (itemId == R.id.nav_sales) {
                    fragment = new AdminSalesFragment();
                } else if (itemId == R.id.nav_withdraw) {
                    fragment = new AdminWithdrawalsFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                }
            } else if (userRole.equals("shop_owner")) {
                if (itemId == R.id.nav_dashboard) {
                    fragment = new ShopDashboardFragment();
                } else if (itemId == R.id.nav_products) {
                    fragment = new ProductsFragment();
                } else if (itemId == R.id.nav_orders) {
                    fragment = new ShopOrdersFragment();
                } else if (itemId == R.id.nav_sales) {
                    fragment = new ShopSalesFragment();
                } else if (itemId == R.id.nav_withdraw) {
                    fragment = new WithdrawFragment();
                } else if (itemId == R.id.nav_myshop) {
                    fragment = new MyShopFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                }
            } else {
                if (itemId == R.id.nav_home) {
                    fragment = new CustomerHomeFragment();
                } else if (itemId == R.id.nav_cart) {
                    fragment = new CartFragment();
                } else if (itemId == R.id.nav_orders) {
                    fragment = new CustomerOrdersFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                }
            }

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Load default fragment
        if (userRole.equals("admin")) {
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
            setupOrderBadge(R.id.nav_dashboard);
        } else if (userRole.equals("shop_owner")) {
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
            setupOrderBadge(R.id.nav_orders);
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            setupCartBadge();
        }
    }

    private void setupAdminNavigation(Menu menu) {
        menu.add(0, R.id.nav_dashboard, 0, "Dashboard")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_dashboard))
                .setCheckable(true);
        menu.add(0, R.id.nav_sales, 1, "Sales")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_orders))
                .setCheckable(true);
        menu.add(0, R.id.nav_withdraw, 2, "Withdrawals")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_profile))
                .setCheckable(true);
        menu.add(0, R.id.nav_profile, 3, "Profile")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_profile))
                .setCheckable(true);
    }

    private void setupShopOwnerNavigation(Menu menu) {
        menu.add(0, R.id.nav_dashboard, 0, "Dashboard")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_dashboard))
                .setCheckable(true);
        menu.add(0, R.id.nav_products, 1, "Products")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_products))
                .setCheckable(true);
        menu.add(0, R.id.nav_orders, 2, "Orders")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_orders))
                .setCheckable(true);
        menu.add(0, R.id.nav_myshop, 3, "My Shop")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_my_shop))
                .setCheckable(true);
        menu.add(0, R.id.nav_withdraw, 4, "Withdraw")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_profile))
                .setCheckable(true);
        menu.add(0, R.id.nav_profile, 5, "Profile")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_profile))
                .setCheckable(true);
    }

    private void setupCustomerNavigation(Menu menu) {
        menu.add(0, R.id.nav_home, 0, "Home")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_dashboard))
                .setCheckable(true);
        menu.add(0, R.id.nav_cart, 1, "Cart")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_cart))
                .setCheckable(true);
        menu.add(0, R.id.nav_orders, 2, "Orders")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_orders))
                .setCheckable(true);
        menu.add(0, R.id.nav_profile, 3, "Profile")
                .setIcon(ContextCompat.getDrawable(this, R.drawable.ic_profile))
                .setCheckable(true);
    }

    private void setupCartBadge() {
        CartManager cartManager = CartManager.getInstance(this);

        bottomNavigationView.post(() -> {
            View cartItem = bottomNavigationView.findViewById(R.id.nav_cart);
            if (cartItem == null) return;

            cartBadgeTextView = new TextView(this);
            cartBadgeTextView.setBackgroundResource(R.drawable.bg_cart_badge);
            cartBadgeTextView.setTextColor(0xFFFFFFFF);
            cartBadgeTextView.setTextSize(10);
            cartBadgeTextView.setTypeface(null, Typeface.BOLD);
            cartBadgeTextView.setGravity(Gravity.CENTER);
            cartBadgeTextView.setVisibility(View.GONE);

            int badgeSize = (int) (18 * getResources().getDisplayMetrics().density + 0.5f);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            params.gravity = Gravity.TOP | Gravity.END;
            params.topMargin = 4;
            params.rightMargin = 4;

            if (cartItem.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) cartItem.getParent();
                parent.addView(cartBadgeTextView, params);
            }

            cartListener = (items, total) -> updateCartBadgeCount();
            cartManager.setCartUpdateListener(cartListener);
            updateCartBadgeCount();
        });
    }

    private void updateCartBadgeCount() {
        if (cartBadgeTextView == null) return;
        int count = CartManager.getInstance(this).getCartCount();
        if (count > 0) {
            cartBadgeTextView.setText(count > 99 ? "99+" : String.valueOf(count));
            cartBadgeTextView.setVisibility(View.VISIBLE);
        } else {
            cartBadgeTextView.setVisibility(View.GONE);
        }
    }

    private void setupOrderBadge(int menuItemId) {
        bottomNavigationView.post(() -> {
            View menuItem = bottomNavigationView.findViewById(menuItemId);
            if (menuItem == null) return;

            orderBadgeTextView = new TextView(this);
            orderBadgeTextView.setBackgroundResource(R.drawable.bg_cart_badge);
            orderBadgeTextView.setTextColor(0xFFFFFFFF);
            orderBadgeTextView.setTextSize(10);
            orderBadgeTextView.setTypeface(null, Typeface.BOLD);
            orderBadgeTextView.setGravity(Gravity.CENTER);
            orderBadgeTextView.setVisibility(View.GONE);

            int badgeSize = (int) (18 * getResources().getDisplayMetrics().density + 0.5f);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            params.gravity = Gravity.TOP | Gravity.END;
            params.topMargin = 4;
            params.rightMargin = 4;

            if (menuItem.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) menuItem.getParent();
                parent.addView(orderBadgeTextView, params);
            }

            loadOrderCount();
        });
    }

    private void loadOrderCount() {
        if (orderBadgeTextView == null) return;

        new Thread(() -> {
            try {
                String token = AuthManager.getInstance(this).getAccessToken();
                int count = 0;

                if (userRole.equals("shop_owner")) {
                    // Count pending/paid orders for this shop owner's shop
                    String userId = SessionManager.getInstance(this).getUserId();
                    String shopQuery = "select=id&owner_id=eq." + userId + "&limit=1";
                    String shopResp = SupabaseApi.getInstance().get("shops", shopQuery, token);
                    ShopId[] shops = new Gson().fromJson(shopResp, ShopId[].class);
                    if (shops != null && shops.length > 0) {
                        String shopId = shops[0].id;
                        String orderQuery = "select=id&shop_id=eq." + shopId + "&status=in.(pending,paid)&order=created_at.desc";
                        String orderResp = SupabaseApi.getInstance().get("orders", orderQuery, token);
                        OrderId[] orders = new Gson().fromJson(orderResp, OrderId[].class);
                        count = orders != null ? orders.length : 0;
                    }
                } else if (userRole.equals("admin")) {
                    // Count all pending/paid orders across all shops
                    String orderQuery = "select=id&status=in.(pending,paid)&order=created_at.desc";
                    String orderResp = SupabaseApi.getInstance().get("orders", orderQuery, token);
                    OrderId[] orders = new Gson().fromJson(orderResp, OrderId[].class);
                    count = orders != null ? orders.length : 0;
                }

                final int finalCount = count;
                runOnUiThread(() -> {
                    if (orderBadgeTextView == null) return;
                    if (finalCount > 0) {
                        orderBadgeTextView.setText(finalCount > 99 ? "99+" : String.valueOf(finalCount));
                        orderBadgeTextView.setVisibility(View.VISIBLE);
                    } else {
                        orderBadgeTextView.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class ShopId {
        String id;
    }

    private static class OrderId {
        String id;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadgeCount();
        loadOrderCount();
    }

    @Override
    protected void onDestroy() {
        if (cartListener != null) {
            CartManager.getInstance(this).setCartUpdateListener(null);
        }
        super.onDestroy();
    }
}
