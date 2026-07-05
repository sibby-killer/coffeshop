-- Admin RLS fix for hardcoded admin (no Supabase JWT)
-- Run this in Supabase SQL Editor

-- SHOPS: allow admin to insert/update (needed for approve flow)
CREATE POLICY "shops_insert_admin" ON shops FOR INSERT
  WITH CHECK (true);

CREATE POLICY "shops_update_admin" ON shops FOR UPDATE
  USING (true);

CREATE POLICY "shops_select_all" ON shops FOR SELECT
  USING (true);

-- SHOP APPLICATIONS: allow admin to read/update all
CREATE POLICY "applications_select_all" ON shop_applications FOR SELECT
  USING (true);

CREATE POLICY "applications_update_all" ON shop_applications FOR UPDATE
  USING (true);

-- PROFILES: allow admin to read/update/delete all
CREATE POLICY "profiles_select_all" ON profiles FOR SELECT
  USING (true);

CREATE POLICY "profiles_update_all" ON profiles FOR UPDATE
  USING (true);

CREATE POLICY "profiles_delete_all" ON profiles FOR DELETE
  USING (true);

-- ORDERS: allow admin to read all
CREATE POLICY "orders_select_all" ON orders FOR SELECT
  USING (true);

-- ORDER ITEMS: allow admin to read all
CREATE POLICY "order_items_select_all" ON order_items FOR SELECT
  USING (true);

-- PRODUCTS: allow admin full access
CREATE POLICY "products_select_all" ON products FOR SELECT
  USING (true);

CREATE POLICY "products_insert_all" ON products FOR INSERT
  WITH CHECK (true);

CREATE POLICY "products_update_all" ON products FOR UPDATE
  USING (true);

CREATE POLICY "products_delete_all" ON products FOR DELETE
  USING (true);
