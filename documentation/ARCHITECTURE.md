# Coffee Shop App v2 — Architecture Document

## Overview
A multi-role coffee shop marketplace Android application where customers browse and order from local coffee shops, shop owners manage their businesses, and admins control platform access.

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend | Java Android (Views + XML) | User interface |
| Auth | Supabase GoTrue | Email/password authentication |
| Database | Supabase PostgreSQL | All data storage |
| Storage | Supabase Storage | Product/shop images |
| Realtime | Supabase Realtime | Live order status updates |
| Payments | Paystack via Edge Functions | M-PESA, Card, Bank payments |
| Local Cache | Room (SQLite) | Offline product browsing |
| Images | Glide | Image loading and caching |

## User Roles

### Admin
- Approves/rejects shop owner applications
- Manages all shops (activate/deactivate)
- Views platform statistics

### Shop Owner
- Submits shop application for admin approval
- Manages shop details and products (CRUD)
- Views and updates order status
- Sees incoming orders in real-time

### Customer
- Browses approved shops and their products
- Adds products to cart
- Pays via Paystack (Card, M-PESA, Bank)
- Tracks order status in real-time

## Application Flow

```
┌─────────────────────────────────────────────────────────┐
│                    REGISTRATION                          │
│  User registers → selects role → profile created         │
│  ├─ customer → can browse immediately                    │
│  └─ shop_owner → must submit application                 │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                 SHOP APPROVAL                            │
│  Shop Owner → submits application → status: pending      │
│  Admin → reviews application → approve/reject            │
│  Approved → shop created → visible to customers          │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  ORDER FLOW                              │
│  Customer → selects shop → adds to cart → checkout       │
│  → Paystack payment → order created (status: paid)       │
│  → Shop owner sees order → status: preparing             │
│  → Shop owner marks ready → status: ready                │
│  → Customer picks up → status: completed                 │
└─────────────────────────────────────────────────────────┘
```

## Order Status Lifecycle

```
pending → paid → preparing → ready → completed
                     ↓
                 cancelled
```

| Status | Set By | Meaning |
|--------|--------|---------|
| pending | System | Order created, awaiting payment |
| paid | Paystack webhook | Payment confirmed |
| preparing | Shop Owner | Shop is making the order |
| ready | Shop Owner | Order ready for pickup |
| completed | Shop Owner | Customer has picked up |
| cancelled | Customer/Shop Owner | Order cancelled |

## Navigation Structure

### Customer
```
Bottom Nav: Home | Cart | Orders
Home → Shop List → Shop Detail → Product Detail → Add to Cart
Cart → Checkout → Paystack Payment → Order Confirmation
Orders → Order Detail (real-time status tracking)
```

### Shop Owner
```
Bottom Nav: Dashboard | Products | Orders
Dashboard → Shop stats, recent orders
Products → List → Add/Edit/Delete Product
Orders → Incoming orders → Update status
Application → Application status / Submit new
```

### Admin
```
Bottom Nav: Dashboard | Applications | Shops
Dashboard → Platform stats
Applications → Pending list → Approve/Reject
Shops → All shops → Activate/Deactivate
```

## Database Schema

See `schema.sql` for full SQL definitions.

### ER Diagram (Text)
```
auth.users ──(1:1)──► profiles
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
  shop_applications    orders           (customer)
        │                 │
        ▼                 ▼
      shops          order_items
        │                 │
        ▼                 ▼
    products          products
```

## Security

### Row Level Security (RLS)
- Every table has RLS enabled
- Policies enforce role-based access
- Customers can only see their own orders
- Shop owners can only manage their own shop/products
- Admin has full access to all tables

### Payment Security
- Paystack **Secret Key** never touches the Android app
- Payment initialization happens in Supabase Edge Functions
- Only the **Public Key** is used on the client side
- Transaction verification is server-side

## Realtime Subscriptions

| Subscription | Who | What |
|-------------|-----|------|
| `orders` | Shop Owner | New incoming orders |
| `orders` | Customer | Order status changes |
| `shop_applications` | Admin | New applications |
| `products` | Customer | Product availability changes |

## File Structure

```
app/src/main/java/com/example/coffeecafe/
├── CoffeeShopApplication.java          — App initialization, Supabase client
├── auth/
│   ├── LoginActivity.java             — Email/password login
│   ├── SignupActivity.java            — Register with role selection
│   └── AuthManager.java               — Session management, role routing
├── models/
│   ├── Profile.java                   — User profile + role
│   ├── Shop.java                      — Shop data
│   ├── ShopApplication.java           — Application data
│   ├── Product.java                   — Product data
│   ├── Order.java                     — Order data
│   └── OrderItem.java                 — Order item data
├── customer/
│   ├── CustomerHomeFragment.java      — Browse approved shops
│   ├── ShopDetailFragment.java        — View shop + products
│   ├── CartFragment.java              — Cart management
│   ├── CheckoutActivity.java          — Paystack payment
│   └── CustomerOrdersFragment.java    — Order tracking
├── shopowner/
│   ├── ShopDashboardFragment.java     — Overview stats
│   ├── MyShopFragment.java            — Edit shop details
│   ├── ProductsFragment.java          — Product CRUD
│   ├── ShopOrdersFragment.java        — Manage orders
│   └── ApplicationStatusFragment.java — Application tracking
├── admin/
│   ├── AdminDashboardFragment.java    — Platform stats
│   ├── ApplicationsFragment.java      — Review applications
│   └── ManageShopsFragment.java       — Shop management
├── adapters/
│   ├── ShopAdapter.java               — Shop list adapter
│   ├── ProductAdapter.java            — Product list adapter
│   ├── CartAdapter.java               — Cart items adapter
│   ├── OrderAdapter.java              — Orders list adapter
│   └── ApplicationAdapter.java        — Applications adapter
├── utils/
│   ├── Constants.java                 — Table names, keys
│   ├── SessionManager.java            — Local session storage
│   └── CartManager.java               — Local cart management
└── config/
    └── SupabaseConfig.java            — Supabase client configuration
```
