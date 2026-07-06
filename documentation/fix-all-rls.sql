-- Fix all RLS and storage issues
-- Run this in Supabase SQL Editor

-- ============================================================
-- 1. FIX ORDERS: Allow customers to insert orders
-- ============================================================
-- Drop existing restrictive policies and recreate permissive ones
DROP POLICY IF EXISTS "orders_customer_insert" ON orders;
DROP POLICY IF EXISTS "orders_customer_read" ON orders;
DROP POLICY IF EXISTS "orders_shop_read" ON orders;
DROP POLICY IF EXISTS "orders_shop_update" ON orders;
DROP POLICY IF EXISTS "orders_admin_read" ON orders;
DROP POLICY IF EXISTS "orders_select_all" ON orders;

-- Permissive: Any authenticated user can insert orders
CREATE POLICY "orders_insert_auth" ON orders
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Permissive: Customers can read their own orders
CREATE POLICY "orders_select_own" ON orders
  FOR SELECT USING (auth.uid() = customer_id);

-- Permissive: Shop owners can read/update orders for their shop
CREATE POLICY "orders_select_shop" ON orders
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = orders.shop_id AND owner_id = auth.uid()
    )
  );

CREATE POLICY "orders_update_shop" ON orders
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = orders.shop_id AND owner_id = auth.uid()
    )
  );

-- Permissive: Admin can read all orders
CREATE POLICY "orders_select_admin" ON orders
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- 2. FIX ORDER ITEMS: Allow customers to insert order items
-- ============================================================
DROP POLICY IF EXISTS "order_items_customer_insert" ON order_items;
DROP POLICY IF EXISTS "order_items_customer_read" ON order_items;
DROP POLICY IF EXISTS "order_items_shop_read" ON order_items;
DROP POLICY IF EXISTS "order_items_admin_read" ON order_items;
DROP POLICY IF EXISTS "order_items_select_all" ON order_items;

-- Permissive: Any authenticated user can insert order items
CREATE POLICY "order_items_insert_auth" ON order_items
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Permissive: Customers can read their own order items
CREATE POLICY "order_items_select_own" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orders WHERE id = order_items.order_id AND customer_id = auth.uid()
    )
  );

-- Permissive: Shop owners can read order items for their shop
CREATE POLICY "order_items_select_shop" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orders o
      JOIN shops s ON o.shop_id = s.id
      WHERE o.id = order_items.order_id AND s.owner_id = auth.uid()
    )
  );

-- Permissive: Admin can read all order items
CREATE POLICY "order_items_select_admin" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- 3. FIX SHOPS: Allow shop creation without application_id
-- ============================================================
-- The shops table requires application_id but we create shops directly
-- Make application_id nullable
ALTER TABLE shops ALTER COLUMN application_id DROP NOT NULL;

-- Drop existing restrictive policies
DROP POLICY IF EXISTS "shops_public_read" ON shops;
DROP POLICY IF EXISTS "shops_owner_read" ON shops;
DROP POLICY IF EXISTS "shops_owner_update" ON shops;
DROP POLICY IF EXISTS "shops_admin_read" ON shops;
DROP POLICY IF EXISTS "shops_admin_update" ON shops;
DROP POLICY IF EXISTS "shops_insert_admin" ON shops;
DROP POLICY IF EXISTS "shops_update_admin" ON shops;
DROP POLICY IF EXISTS "shops_select_all" ON shops;

-- Permissive: Anyone can read active shops
CREATE POLICY "shops_select_active" ON shops
  FOR SELECT USING (is_active = true);

-- Permissive: Shop owners can read their own shop
CREATE POLICY "shops_select_own" ON shops
  FOR SELECT USING (auth.uid() = owner_id);

-- Permissive: Shop owners can update their own shop
CREATE POLICY "shops_update_own" ON shops
  FOR UPDATE USING (auth.uid() = owner_id);

-- Permissive: Any authenticated user can create a shop
CREATE POLICY "shops_insert_auth" ON shops
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Permissive: Admin can do everything with shops
CREATE POLICY "shops_all_admin" ON shops
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- 4. FIX PRODUCTS: Allow shop owners to insert products
-- ============================================================
DROP POLICY IF EXISTS "products_public_read" ON products;
DROP POLICY IF EXISTS "products_owner_all" ON products;
DROP POLICY IF EXISTS "products_admin_read" ON products;
DROP POLICY IF EXISTS "products_select_all" ON products;
DROP POLICY IF EXISTS "products_insert_all" ON products;
DROP POLICY IF EXISTS "products_update_all" ON products;
DROP POLICY IF EXISTS "products_delete_all" ON products;

-- Permissive: Anyone can read products from active shops
CREATE POLICY "products_select_active" ON products
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = products.shop_id AND is_active = true
    )
  );

-- Permissive: Shop owners can manage their own products
CREATE POLICY "products_manage_own" ON products
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = products.shop_id AND owner_id = auth.uid()
    )
  );

-- Permissive: Admin can do everything with products
CREATE POLICY "products_all_admin" ON products
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- 5. FIX PROFILES: Allow all authenticated users to read profiles
-- ============================================================
DROP POLICY IF EXISTS "profiles_select_own" ON profiles;
DROP POLICY IF EXISTS "profiles_update_own" ON profiles;
DROP POLICY IF EXISTS "profiles_admin_read" ON profiles;
DROP POLICY IF EXISTS "profiles_select_all" ON profiles;
DROP POLICY IF EXISTS "profiles_update_all" ON profiles;
DROP POLICY IF EXISTS "profiles_delete_all" ON profiles;

-- Permissive: Users can read their own profile
CREATE POLICY "profiles_select_own" ON profiles
  FOR SELECT USING (auth.uid() = id);

-- Permissive: Users can update their own profile
CREATE POLICY "profiles_update_own" ON profiles
  FOR UPDATE USING (auth.uid() = id);

-- Permissive: Admin can read/update all profiles
CREATE POLICY "profiles_all_admin" ON profiles
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- 6. FIX SHOP APPLICATIONS: Allow shop owners to create applications
-- ============================================================
DROP POLICY IF EXISTS "applications_owner_read" ON shop_applications;
DROP POLICY IF EXISTS "applications_owner_insert" ON shop_applications;
DROP POLICY IF EXISTS "applications_admin_read" ON shop_applications;
DROP POLICY IF EXISTS "applications_admin_update" ON shop_applications;
DROP POLICY IF EXISTS "applications_select_all" ON shop_applications;
DROP POLICY IF EXISTS "applications_update_all" ON shop_applications;

-- Permissive: Shop owners can read their own applications
CREATE POLICY "applications_select_own" ON shop_applications
  FOR SELECT USING (auth.uid() = owner_id);

-- Permissive: Shop owners can create applications
CREATE POLICY "applications_insert_own" ON shop_applications
  FOR INSERT WITH CHECK (auth.uid() = owner_id);

-- Permissive: Admin can read/update all applications
CREATE POLICY "applications_all_admin" ON shop_applications
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- 7. CREATE STORAGE BUCKETS
-- ============================================================
-- Create buckets if they don't exist
INSERT INTO storage.buckets (id, name, public) VALUES ('products', 'products', true)
  ON CONFLICT (id) DO NOTHING;

INSERT INTO storage.buckets (id, name, public) VALUES ('shops', 'shops', true)
  ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. STORAGE POLICIES
-- ============================================================
-- Drop existing storage policies
DROP POLICY IF EXISTS "products_select_public" ON storage.objects;
DROP POLICY IF EXISTS "products_insert_auth" ON storage.objects;
DROP POLICY IF EXISTS "shops_select_public" ON storage.objects;
DROP POLICY IF EXISTS "shops_insert_auth" ON storage.objects;

-- Products bucket: anyone can view, authenticated can upload
CREATE POLICY "products_select_public" ON storage.objects
  FOR SELECT USING (bucket_id = 'products');

CREATE POLICY "products_insert_auth" ON storage.objects
  FOR INSERT WITH CHECK (bucket_id = 'products' AND auth.role() = 'authenticated');

CREATE POLICY "products_update_auth" ON storage.objects
  FOR UPDATE USING (bucket_id = 'products' AND auth.role() = 'authenticated');

CREATE POLICY "products_delete_auth" ON storage.objects
  FOR DELETE USING (bucket_id = 'products' AND auth.role() = 'authenticated');

-- Shops bucket: anyone can view, authenticated can upload
CREATE POLICY "shops_select_public" ON storage.objects
  FOR SELECT USING (bucket_id = 'shops');

CREATE POLICY "shops_insert_auth" ON storage.objects
  FOR INSERT WITH CHECK (bucket_id = 'shops' AND auth.role() = 'authenticated');

CREATE POLICY "shops_update_auth" ON storage.objects
  FOR UPDATE USING (bucket_id = 'shops' AND auth.role() = 'authenticated');

CREATE POLICY "shops_delete_auth" ON storage.objects
  FOR DELETE USING (bucket_id = 'shops' AND auth.role() = 'authenticated');
