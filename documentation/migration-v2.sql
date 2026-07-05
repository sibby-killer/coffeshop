-- Migration: Add quantity to products + withdrawals table + RLS
-- Run this in Supabase SQL Editor

-- ============================================
-- 1. Add quantity column to products
-- ============================================
ALTER TABLE products ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 0;

-- ============================================
-- 2. Create withdrawals table
-- ============================================
CREATE TABLE IF NOT EXISTS withdrawals (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  shop_owner_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  full_name TEXT NOT NULL,
  payment_method TEXT NOT NULL CHECK (payment_method IN ('mpesa', 'airtel_money', 'bank')),
  account_number TEXT NOT NULL,
  amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
  status TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_withdrawals_owner ON withdrawals(shop_owner_id);
CREATE INDEX IF NOT EXISTS idx_withdrawals_status ON withdrawals(status);

-- Trigger for updated_at
CREATE TRIGGER update_withdrawals_updated_at
  BEFORE UPDATE ON withdrawals
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================
-- 3. Enable RLS on withdrawals
-- ============================================
ALTER TABLE withdrawals ENABLE ROW LEVEL SECURITY;

-- Shop owners can read their own withdrawals
CREATE POLICY "withdrawals_select_own" ON withdrawals
  FOR SELECT USING (auth.uid() = shop_owner_id);

-- Shop owners can create withdrawals
CREATE POLICY "withdrawals_insert_own" ON withdrawals
  FOR INSERT WITH CHECK (auth.uid() = shop_owner_id);

-- Admin can read all withdrawals
CREATE POLICY "withdrawals_select_admin" ON withdrawals
  FOR SELECT USING (true);

-- Admin can update all withdrawals (approve/reject)
CREATE POLICY "withdrawals_update_all" ON withdrawals
  FOR UPDATE USING (true);

-- ============================================
-- 4. Grant permissions
-- ============================================
GRANT ALL ON withdrawals TO anon, authenticated;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
