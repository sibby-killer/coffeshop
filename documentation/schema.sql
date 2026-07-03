-- ============================================================
-- Coffee Shop App v2 — Supabase PostgreSQL Schema
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. PROFILES TABLE (extends auth.users)
-- ============================================================
CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT NOT NULL DEFAULT '',
  phone TEXT NOT NULL DEFAULT '',
  role TEXT NOT NULL CHECK (role IN ('admin', 'shop_owner', 'customer')) DEFAULT 'customer',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auto-create profile on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, phone, role)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'full_name', ''),
    COALESCE(NEW.raw_user_meta_data->>'phone', ''),
    COALESCE(NEW.raw_user_meta_data->>'role', 'customer')
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create profile
CREATE OR REPLACE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================
-- 2. SHOP APPLICATIONS TABLE
-- ============================================================
CREATE TABLE shop_applications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  owner_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  shop_name TEXT NOT NULL,
  shop_description TEXT NOT NULL DEFAULT '',
  location TEXT NOT NULL DEFAULT '',
  phone TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
  admin_notes TEXT DEFAULT '',
  reviewed_by UUID REFERENCES profiles(id),
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 3. SHOPS TABLE
-- ============================================================
CREATE TABLE shops (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  application_id UUID NOT NULL REFERENCES shop_applications(id) ON DELETE CASCADE,
  owner_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  location TEXT NOT NULL DEFAULT '',
  phone TEXT NOT NULL DEFAULT '',
  image_url TEXT DEFAULT '',
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 4. PRODUCTS TABLE
-- ============================================================
CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
  category TEXT NOT NULL DEFAULT 'general',
  image_url TEXT DEFAULT '',
  is_available BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 5. ORDERS TABLE
-- ============================================================
CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  customer_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
  total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
  status TEXT NOT NULL CHECK (status IN ('pending', 'paid', 'preparing', 'ready', 'completed', 'cancelled')) DEFAULT 'pending',
  payment_reference TEXT DEFAULT '',
  payment_method TEXT DEFAULT '',
  notes TEXT DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 6. ORDER ITEMS TABLE
-- ============================================================
CREATE TABLE order_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  product_name TEXT NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_profiles_role ON profiles(role);
CREATE INDEX idx_shop_applications_owner ON shop_applications(owner_id);
CREATE INDEX idx_shop_applications_status ON shop_applications(status);
CREATE INDEX idx_shops_owner ON shops(owner_id);
CREATE INDEX idx_shops_active ON shops(is_active);
CREATE INDEX idx_products_shop ON products(shop_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_shop ON orders(shop_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- ============================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================

-- Enable RLS on all tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- PROFILES POLICIES
-- ============================================================

-- Anyone can read their own profile
CREATE POLICY "profiles_select_own" ON profiles
  FOR SELECT USING (auth.uid() = id);

-- Users can update their own profile
CREATE POLICY "profiles_update_own" ON profiles
  FOR UPDATE USING (auth.uid() = id);

-- Admin can read all profiles
CREATE POLICY "profiles_admin_read" ON profiles
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- SHOP APPLICATIONS POLICIES
-- ============================================================

-- Shop owners can read their own applications
CREATE POLICY "applications_owner_read" ON shop_applications
  FOR SELECT USING (auth.uid() = owner_id);

-- Shop owners can create applications
CREATE POLICY "applications_owner_insert" ON shop_applications
  FOR INSERT WITH CHECK (auth.uid() = owner_id);

-- Admin can read all applications
CREATE POLICY "applications_admin_read" ON shop_applications
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- Admin can update applications (approve/reject)
CREATE POLICY "applications_admin_update" ON shop_applications
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- SHOPS POLICIES
-- ============================================================

-- Everyone can read active shops (customers need to browse)
CREATE POLICY "shops_public_read" ON shops
  FOR SELECT USING (is_active = true);

-- Shop owners can read their own shop (even if inactive)
CREATE POLICY "shops_owner_read" ON shops
  FOR SELECT USING (auth.uid() = owner_id);

-- Shop owners can update their own shop
CREATE POLICY "shops_owner_update" ON shops
  FOR UPDATE USING (auth.uid() = owner_id);

-- Admin can read all shops
CREATE POLICY "shops_admin_read" ON shops
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- Admin can update all shops
CREATE POLICY "shops_admin_update" ON shops
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- PRODUCTS POLICIES
-- ============================================================

-- Everyone can read products from active shops
CREATE POLICY "products_public_read" ON products
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = products.shop_id AND is_active = true
    )
  );

-- Shop owners can manage their own products
CREATE POLICY "products_owner_all" ON products
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = products.shop_id AND owner_id = auth.uid()
    )
  );

-- Admin can read all products
CREATE POLICY "products_admin_read" ON products
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- ORDERS POLICIES
-- ============================================================

-- Customers can read their own orders
CREATE POLICY "orders_customer_read" ON orders
  FOR SELECT USING (auth.uid() = customer_id);

-- Customers can create orders
CREATE POLICY "orders_customer_insert" ON orders
  FOR INSERT WITH CHECK (auth.uid() = customer_id);

-- Shop owners can read orders for their shop
CREATE POLICY "orders_shop_read" ON orders
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = orders.shop_id AND owner_id = auth.uid()
    )
  );

-- Shop owners can update order status for their shop
CREATE POLICY "orders_shop_update" ON orders
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM shops WHERE id = orders.shop_id AND owner_id = auth.uid()
    )
  );

-- Admin can read all orders
CREATE POLICY "orders_admin_read" ON orders
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- ORDER ITEMS POLICIES
-- ============================================================

-- Customers can read their own order items
CREATE POLICY "order_items_customer_read" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orders WHERE id = order_items.order_id AND customer_id = auth.uid()
    )
  );

-- Customers can create order items
CREATE POLICY "order_items_customer_insert" ON order_items
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM orders WHERE id = order_items.order_id AND customer_id = auth.uid()
    )
  );

-- Shop owners can read order items for their shop
CREATE POLICY "order_items_shop_read" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orders o
      JOIN shops s ON o.shop_id = s.id
      WHERE o.id = order_items.order_id AND s.owner_id = auth.uid()
    )
  );

-- Admin can read all order items
CREATE POLICY "order_items_admin_read" ON order_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND role = 'admin'
    )
  );

-- ============================================================
-- UPDATED_AT TRIGGERS
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_updated_at
  BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_shop_applications_updated_at
  BEFORE UPDATE ON shop_applications
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_shops_updated_at
  BEFORE UPDATE ON shops
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_products_updated_at
  BEFORE UPDATE ON products
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_orders_updated_at
  BEFORE UPDATE ON orders
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================================
-- SEED DATA (Optional Admin User)
-- ============================================================

-- To create an admin user, register with this email and manually update role:
-- UPDATE profiles SET role = 'admin' WHERE id = '<user-uuid>';
