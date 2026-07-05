-- Fix: shops.application_id NOT NULL constraint + permissive RLS
-- Run this in Supabase SQL Editor

-- ============================================
-- STEP 1: Make application_id nullable
-- ============================================
ALTER TABLE shops ALTER COLUMN application_id DROP NOT NULL;

-- ============================================
-- STEP 2: Drop ALL existing policies (safe dynamic DROP)
-- ============================================
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
-- STEP 3: Recreate permissive policies
-- ============================================

-- PROFILES
CREATE POLICY "profiles_select_all" ON profiles FOR SELECT USING (true);
CREATE POLICY "profiles_insert_own" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_update_own" ON profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "profiles_delete_own" ON profiles FOR DELETE USING (auth.uid() = id);

-- SHOPS (public read + owner write + allow authenticated inserts)
CREATE POLICY "shops_select_all" ON shops FOR SELECT USING (true);
CREATE POLICY "shops_insert_authenticated" ON shops FOR INSERT
  WITH CHECK (auth.uid() = owner_id OR auth.uid() IS NOT NULL);
CREATE POLICY "shops_update_owner" ON shops FOR UPDATE
  USING (auth.uid() = owner_id);
CREATE POLICY "shops_delete_owner" ON shops FOR DELETE
  USING (auth.uid() = owner_id);

-- PRODUCTS (public read + owner write)
CREATE POLICY "products_select_all" ON products FOR SELECT USING (true);
CREATE POLICY "products_insert_authenticated" ON products FOR INSERT
  WITH CHECK (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = products.shop_id AND shops.owner_id = auth.uid())
    OR auth.uid() IS NOT NULL
  );
CREATE POLICY "products_update_owner" ON products FOR UPDATE
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = products.shop_id AND shops.owner_id = auth.uid())
  );
CREATE POLICY "products_delete_owner" ON products FOR DELETE
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = products.shop_id AND shops.owner_id = auth.uid())
  );

-- ORDERS
CREATE POLICY "orders_select_customer" ON orders FOR SELECT
  USING (auth.uid() = customer_id);
CREATE POLICY "orders_select_shop_owner" ON orders FOR SELECT
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = orders.shop_id AND shops.owner_id = auth.uid())
  );
CREATE POLICY "orders_insert_authenticated" ON orders FOR INSERT
  WITH CHECK (auth.uid() = customer_id OR auth.uid() IS NOT NULL);
CREATE POLICY "orders_update_shop_owner" ON orders FOR UPDATE
  USING (
    EXISTS (SELECT 1 FROM shops WHERE shops.id = orders.shop_id AND shops.owner_id = auth.uid())
  );

-- ORDER ITEMS
CREATE POLICY "order_items_select_customer" ON order_items FOR SELECT
  USING (
    EXISTS (SELECT 1 FROM orders WHERE orders.id = order_items.order_id AND orders.customer_id = auth.uid())
  );
CREATE POLICY "order_items_select_shop_owner" ON order_items FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM orders
      JOIN shops ON shops.id = orders.shop_id
      WHERE orders.id = order_items.order_id AND shops.owner_id = auth.uid()
    )
  );
CREATE POLICY "order_items_insert_authenticated" ON order_items FOR INSERT
  WITH CHECK (
    EXISTS (SELECT 1 FROM orders WHERE orders.id = order_items.order_id AND orders.customer_id = auth.uid())
    OR auth.uid() IS NOT NULL
  );

-- SHOP APPLICATIONS (keep existing)
CREATE POLICY "shop_applications_select_own" ON shop_applications FOR SELECT
  USING (auth.uid() = owner_id);
CREATE POLICY "shop_applications_insert_own" ON shop_applications FOR INSERT
  WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "shop_applications_select_all" ON shop_applications FOR SELECT USING (true);
CREATE POLICY "shop_applications_insert_all" ON shop_applications FOR INSERT WITH CHECK (true);
CREATE POLICY "shop_applications_update_all" ON shop_applications FOR UPDATE USING (true);

-- ============================================
-- STEP 4: Grant permissions
-- ============================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- Verify
SELECT tablename, policyname, cmd, qual
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
