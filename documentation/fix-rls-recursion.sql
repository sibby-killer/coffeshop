-- FIX: Infinite recursion in profiles RLS policies
-- Run this in Supabase SQL Editor AFTER fix-all-rls.sql
-- 
-- PROBLEM: profiles_all_admin policy queries profiles from within profiles = infinite recursion
-- This breaks ALL tables because every admin policy references profiles.
--
-- FIX: Create a SECURITY DEFINER function that bypasses RLS,
-- then use it in all admin policies.

-- ============================================================
-- 1. Create SECURITY DEFINER function to check admin role
-- ============================================================
-- This function runs as the function owner (postgres), bypassing RLS.
-- It can read profiles without triggering the recursive policy.
CREATE OR REPLACE FUNCTION public.is_admin(uid uuid)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM profiles WHERE id = uid AND role = 'admin'
  );
$$;

-- ============================================================
-- 2. Fix PROFILES: Remove self-referencing policy
-- ============================================================
DROP POLICY IF EXISTS "profiles_all_admin" ON profiles;
DROP POLICY IF EXISTS "profiles_select_own" ON profiles;
DROP POLICY IF EXISTS "profiles_update_own" ON profiles;

-- Users can read their own profile
CREATE POLICY "profiles_select_own" ON profiles
  FOR SELECT USING (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "profiles_update_own" ON profiles
  FOR UPDATE USING (auth.uid() = id);

-- Admin can read all profiles (uses SECURITY DEFINER function, no recursion)
CREATE POLICY "profiles_admin_all" ON profiles
  FOR ALL USING (public.is_admin(auth.uid()));

-- ============================================================
-- 3. Fix SHOPS: Use is_admin() instead of profiles subquery
-- ============================================================
DROP POLICY IF EXISTS "shops_select_active" ON shops;
DROP POLICY IF EXISTS "shops_select_own" ON shops;
DROP POLICY IF EXISTS "shops_update_own" ON shops;
DROP POLICY IF EXISTS "shops_insert_auth" ON shops;
DROP POLICY IF EXISTS "shops_all_admin" ON shops;

-- Anyone can read active shops
CREATE POLICY "shops_select_active" ON shops
  FOR SELECT USING (is_active = true);

-- Shop owners can read their own shop
CREATE POLICY "shops_select_own" ON shops
  FOR SELECT USING (auth.uid() = owner_id);

-- Shop owners can update their own shop
CREATE POLICY "shops_update_own" ON shops
  FOR UPDATE USING (auth.uid() = owner_id);

-- Any authenticated user can create a shop
CREATE POLICY "shops_insert_auth" ON shops
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Admin can do everything with shops
CREATE POLICY "shops_admin_all" ON shops
  FOR ALL USING (public.is_admin(auth.uid()));

-- ============================================================
-- 4. Fix ORDERS: Use is_admin() instead of profiles subquery
-- ============================================================
DROP POLICY IF EXISTS "orders_insert_auth" ON orders;
DROP POLICY IF EXISTS "orders_select_own" ON orders;
DROP POLICY IF EXISTS "orders_select_shop" ON orders;
DROP POLICY IF EXISTS "orders_update_shop" ON orders;
DROP POLICY IF EXISTS "orders_select_admin" ON orders;

-- Any authenticated user can insert orders
CREATE POLICY "orders_insert_auth" ON orders
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Customers can read their own orders
CREATE POLICY "orders_select_own" ON orders
  FOR SELECT USING (auth.uid() = customer_id);

-- Shop owners can read orders for their shop
CREATE POLICY "orders_select_shop" ON orders
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = orders.shop_id AND owner_id = auth.uid()
    )
  );

-- Shop owners can update orders for their shop
CREATE POLICY "orders_update_shop" ON orders
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = orders.shop_id AND owner_id = auth.uid()
    )
  );

-- Admin can read all orders
CREATE POLICY "orders_admin_all" ON orders
  FOR SELECT USING (public.is_admin(auth.uid()));

-- ============================================================
-- 5. Fix ORDER ITEMS: Use is_admin() instead of profiles subquery
-- ============================================================
DROP POLICY IF EXISTS "order_items_insert_auth" ON order_items;
DROP POLICY IF EXISTS "order_items_select_own" ON order_items;
DROP POLICY IF EXISTS "order_items_select_shop" ON order_items;
DROP POLICY IF EXISTS "order_items_select_admin" ON order_items;

-- Any authenticated user can insert order items
CREATE POLICY "order_items_insert_auth" ON order_items
  FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Customers can read their own order items
CREATE POLICY "order_items_select_own" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orders WHERE id = order_items.order_id AND customer_id = auth.uid()
    )
  );

-- Shop owners can read order items for their shop
CREATE POLICY "order_items_select_shop" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orders o
      JOIN shops s ON o.shop_id = s.id
      WHERE o.id = order_items.order_id AND s.owner_id = auth.uid()
    )
  );

-- Admin can read all order items
CREATE POLICY "order_items_admin_all" ON order_items
  FOR SELECT USING (public.is_admin(auth.uid()));

-- ============================================================
-- 6. Fix PRODUCTS: Use is_admin() instead of profiles subquery
-- ============================================================
DROP POLICY IF EXISTS "products_select_active" ON products;
DROP POLICY IF EXISTS "products_manage_own" ON products;
DROP POLICY IF EXISTS "products_all_admin" ON products;

-- Anyone can read products from active shops
CREATE POLICY "products_select_active" ON products
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = products.shop_id AND is_active = true
    )
  );

-- Shop owners can manage their own products
CREATE POLICY "products_manage_own" ON products
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = products.shop_id AND owner_id = auth.uid()
    )
  );

-- Admin can do everything with products
CREATE POLICY "products_admin_all" ON products
  FOR ALL USING (public.is_admin(auth.uid()));

-- ============================================================
-- 7. Fix SHOP APPLICATIONS: Use is_admin() instead of profiles subquery
-- ============================================================
DROP POLICY IF EXISTS "applications_select_own" ON shop_applications;
DROP POLICY IF EXISTS "applications_insert_own" ON shop_applications;
DROP POLICY IF EXISTS "applications_all_admin" ON shop_applications;

-- Shop owners can read their own applications
CREATE POLICY "applications_select_own" ON shop_applications
  FOR SELECT USING (auth.uid() = owner_id);

-- Shop owners can create applications
CREATE POLICY "applications_insert_own" ON shop_applications
  FOR INSERT WITH CHECK (auth.uid() = owner_id);

-- Admin can read/update all applications
CREATE POLICY "applications_admin_all" ON shop_applications
  FOR ALL USING (public.is_admin(auth.uid()));
