-- Run this in Supabase SQL Editor to fix Data API access
-- (needed because auto-expose was off when project was created)

GRANT USAGE ON SCHEMA public TO anon, authenticated;

GRANT SELECT, INSERT, UPDATE, DELETE ON profiles TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON shop_applications TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON shops TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON products TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON orders TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON order_items TO anon, authenticated;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
