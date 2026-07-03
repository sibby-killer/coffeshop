-- RLS Fix for Supabase
-- Run this in Supabase SQL Editor to fix the "infinite recursion" error
-- Uses dynamic DROP to catch ALL policy names, even partial runs

-- Step 1: Drop ALL existing policies dynamically
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN (
    SELECT schemaname, tablename, policyname
    FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename IN ('profiles', 'shops', 'products', 'orders', 'order_items', 'shop_applications')
  ) LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I', r.policyname, r.tablename);
  END LOOP;
END $$;

-- ============================================
-- RECREATE POLICIES (NO RECURSION!)
-- Key: Use auth.uid() directly, NOT a subquery to profiles table
-- ============================================

-- PROFILES policies
CREATE POLICY "profiles_select_own"
  ON profiles FOR SELECT
  USING (auth.uid() = id);

CREATE POLICY "profiles_insert_own"
  ON profiles FOR INSERT
  WITH CHECK (auth.uid() = id);

CREATE POLICY "profiles_update_own"
  ON profiles FOR UPDATE
  USING (auth.uid() = id);

-- Allow anon to read profiles (needed for order displays)
CREATE POLICY "profiles_select_anon"
  ON profiles FOR SELECT
  USING (true);

-- SHOPS policies
CREATE POLICY "shops_select_public"
  ON shops FOR SELECT
  USING (true);

CREATE POLICY "shops_insert_owner"
  ON shops FOR INSERT
  WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "shops_update_owner"
  ON shops FOR UPDATE
  USING (auth.uid() = owner_id);

CREATE POLICY "shops_delete_owner"
  ON shops FOR DELETE
  USING (auth.uid() = owner_id);

-- PRODUCTS policies
CREATE POLICY "products_select_public"
  ON products FOR SELECT
  USING (true);

CREATE POLICY "products_insert_owner"
  ON products FOR INSERT
  WITH CHECK (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = products.shop_id AND shops.owner_id = auth.uid())
  );

CREATE POLICY "products_update_owner"
  ON products FOR UPDATE
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = products.shop_id AND shops.owner_id = auth.uid())
  );

CREATE POLICY "products_delete_owner"
  ON products FOR DELETE
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = products.shop_id AND shops.owner_id = auth.uid())
  );

-- ORDERS policies
CREATE POLICY "orders_select_customer"
  ON orders FOR SELECT
  USING (auth.uid() = customer_id);

CREATE POLICY "orders_select_shop_owner"
  ON orders FOR SELECT
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = orders.shop_id AND shops.owner_id = auth.uid())
  );

CREATE POLICY "orders_insert_customer"
  ON orders FOR INSERT
  WITH CHECK (auth.uid() = customer_id);

CREATE POLICY "orders_update_shop_owner"
  ON orders FOR UPDATE
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = orders.shop_id AND shops.owner_id = auth.uid())
  );

-- ORDER ITEMS policies
CREATE POLICY "order_items_select_customer"
  ON order_items FOR SELECT
  USING (
    EXISTS (SELECT 1 FROM orders WHERE orders.id = order_items.order_id AND orders.customer_id = auth.uid())
  );

CREATE POLICY "order_items_select_shop_owner"
  ON order_items FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM orders
      JOIN shops ON shops.id = orders.shop_id
      WHERE orders.id = order_items.order_id AND shops.owner_id = auth.uid()
    )
  );

CREATE POLICY "order_items_insert_customer"
  ON order_items FOR INSERT
  WITH CHECK (
    EXISTS (SELECT 1 FROM orders WHERE orders.id = order_items.order_id AND orders.customer_id = auth.uid())
  );

-- SHOP APPLICATIONS policies
CREATE POLICY "shop_applications_select_own"
  ON shop_applications FOR SELECT
  USING (auth.uid() = owner_id);

CREATE POLICY "shop_applications_insert_own"
  ON shop_applications FOR INSERT
  WITH CHECK (auth.uid() = owner_id);

-- Allow anon to insert shop_applications (for signup flow with email confirmation)
CREATE POLICY "shop_applications_insert_anon"
  ON shop_applications FOR INSERT
  WITH CHECK (true);

-- Allow anon to read shop_applications (for application status check)
CREATE POLICY "shop_applications_select_anon"
  ON shop_applications FOR SELECT
  USING (true);

-- Allow anon to update shop_applications (for admin operations)
CREATE POLICY "shop_applications_update_anon"
  ON shop_applications FOR UPDATE
  USING (true);

-- ============================================
-- GRANT PERMISSIONS
-- ============================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- Verify: Check that policies exist and no recursion
SELECT schemaname, tablename, policyname, cmd, qual
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
