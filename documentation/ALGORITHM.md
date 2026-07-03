# Coffee Shop App v2 — Algorithm & Data Structure Documentation

## Overview
This document describes the algorithms, data structures, and design patterns used in the Coffee Shop App, explaining **what** is used and **why**.

---

## 1. State Machine — Order Status Management

### Algorithm
The order lifecycle is implemented as a **Finite State Machine (FSM)**.

### States
```
PENDING ──► PAID ──► PREPARING ──► READY ──► COMPLETED
                │         │
                ▼         ▼
            CANCELLED  CANCELLED
```

### Transition Rules
| Current State | Next State | Trigger | Allowed Roles |
|--------------|-----------|---------|---------------|
| PENDING | PAID | Paystack verification | System (Edge Function) |
| PAID | PREPARING | Shop owner action | Shop Owner |
| PREPARING | READY | Shop owner action | Shop Owner |
| READY | COMPLETED | Shop owner action | Shop Owner |
| PAID | CANCELLED | Customer/Shop owner | Customer, Shop Owner |
| PREPARING | CANCELLED | Shop owner action | Shop Owner |

### Why FSM?
- **Predictability**: Every order has a clear, enforceable flow
- **Validation**: Invalid transitions are rejected at the database level (CHECK constraint)
- **Auditability**: Status history can be tracked
- **UI Simplification**: App only shows valid actions based on current state

### Implementation
```sql
-- Database constraint enforces valid statuses
status TEXT CHECK (status IN ('pending','paid','preparing','ready','completed','cancelled'))
```

---

## 2. Role-Based Access Control (RBAC)

### Algorithm
**Role-based access** with **Row Level Security (RLS)** policies.

### Three-Tier Model
```
┌─────────────────────────────────────────┐
│  Role: admin                            │
│  Permissions: ALL tables, ALL operations│
├─────────────────────────────────────────┤
│  Role: shop_owner                       │
│  Permissions: Own shop, own products,   │
│               own shop's orders         │
├─────────────────────────────────────────┤
│  Role: customer                         │
│  Permissions: All approved shops,       │
│               own cart, own orders      │
└─────────────────────────────────────────┘
```

### Why RBAC?
- **Security**: Users can only access data they own
- **Simplicity**: Three clear roles instead of complex permission matrices
- **Database-Level Enforcement**: RLS policies prevent data leaks even if app code has bugs
- **Scalability**: New roles can be added without rewriting app logic

### Implementation
```sql
-- Example: Shop owner can only see their own products
CREATE POLICY "shop_owner_products" ON products
  FOR ALL USING (
    shop_id IN (
      SELECT id FROM shops WHERE owner_id = auth.uid()
    )
  );
```

---

## 3. Optimistic UI Updates

### Algorithm
**Optimistic updates** for cart and order operations — the UI updates immediately, then syncs with the server.

### Flow
```
User taps "Add to Cart"
  → UI updates instantly (local state)
  → Background: POST to Supabase
  → On success: nothing (already updated)
  → On failure: revert UI, show error toast
```

### Why Optimistic UI?
- **Perceived Performance**: App feels instant, no loading spinners
- **Better UX**: User doesn't wait for network round-trip
- **Offline Resilience**: Works with local cache when network is slow
- **Conflict Resolution**: Last-write-wins for cart items (simple, sufficient)

### Trade-offs
- Possible brief inconsistency if server rejects the operation
- Mitigated by error handling and UI revert

---

## 4. Cart Management — Local-First with Room

### Data Structure
```java
// In-memory HashMap for fast O(1) lookups
HashMap<String, CartItem> cartItems;

// Persisted to Room database for offline access
@Entity
class CartItem {
    @PrimaryKey
    String productId;
    String shopId;
    String productName;
    double price;
    int quantity;
    String imageUrl;
}
```

### Algorithm: Add to Cart
```
1. Check if product already in cart (HashMap lookup - O(1))
2. If exists: increment quantity
3. If not: create new CartItem, add to HashMap
4. Persist to Room database (background thread)
5. Update UI (observe LiveData from Room)
```

### Algorithm: Remove from Cart
```
1. Remove from HashMap (O(1))
2. Delete from Room database (background thread)
3. Update UI
```

### Algorithm: Calculate Total
```
1. Iterate through cart items (O(n))
2. Sum: price × quantity for each item
3. Return total
```

### Why Room + Local-First?
- **Offline Support**: Cart persists even without internet
- **Fast**: Local reads are instant (no network latency)
- **Observable**: Room LiveData auto-updates UI on data changes
- **Reliable**: Survives app restarts and process death

---

## 5. Real-time Order Tracking — Supabase Realtime

### Algorithm
**WebSocket subscription** for live order status updates.

### Flow
```
Customer opens Orders tab
  → Subscribe to Supabase Realtime on 'orders' table
  → Filter: customer_id = current user
  → On INSERT/UPDATE: callback fires
  → Update RecyclerView adapter with new data
  → UI updates automatically
```

### Why Realtime?
- **No Polling**: Instead of querying every N seconds, server pushes updates
- **Bandwidth Efficient**: Only changed data is sent
- **Immediate**: Customer sees status change within milliseconds
- **Reduced Server Load**: No repeated queries from multiple clients

### Implementation Pattern
```java
// Subscribe to order changes
supabase.channel("orders")
    .onPostgresChanges(
        event = EventType.UPDATE,
        table = "orders",
        filter = "customer_id=eq." + userId,
        callback = (payload) -> {
            runOnUiThread(() -> {
                // Update order in adapter
                Order updated = parseOrder(payload);
                adapter.updateOrder(updated);
            });
        }
    )
    .subscribe();
```

---

## 6. Image Loading & Caching — Glide

### Algorithm
**Three-tier caching** for efficient image loading.

### Cache Levels
```
┌─────────────────────────────────┐
│  Level 1: Memory Cache          │
│  - Fastest (microseconds)       │
│  - LRU eviction (limited size)  │
├─────────────────────────────────┤
│  Level 2: Disk Cache            │
│  - Fast (milliseconds)          │
│  - Persists across app restarts │
├─────────────────────────────────┤
│  Level 3: Network               │
│  - Slowest (hundreds of ms)     │
│  - Downloaded once, then cached │
└─────────────────────────────────┘
```

### Loading Flow
```
1. Check memory cache → if found, display immediately
2. Check disk cache → if found, display + store in memory
3. Download from network → display + store in memory + disk
```

### Why Glide?
- **Automatic Caching**: No manual cache management needed
- **Lifecycle-Aware**: Pauses loading when activity/fragment is paused
- **Placeholder Support**: Shows loading spinner or error image
- **Memory Efficient**: Automatic downsampling for large images
- **Well-Tested**: Industry standard for Android image loading

---

## 7. Authentication Flow — Supabase GoTrue

### Algorithm
**Token-based authentication** with automatic refresh.

### Flow
```
┌─────────────────────────────────────────┐
│  FIRST LAUNCH                           │
│  1. Check for stored session             │
│  2. If no session → Login/Signup screen  │
│  3. If session exists → Validate token   │
│  4. If token valid → Dashboard           │
│  5. If token expired → Refresh token     │
│  6. If refresh fails → Login screen      │
└─────────────────────────────────────────┘
```

### Token Management
```
Access Token: expires in 1 hour
  → Stored in SharedPreferences (encrypted)
  → Sent with every API request (Authorization header)
  → Auto-refreshed by Supabase client

Refresh Token: expires in 30 days
  → Stored in SharedPreferences (encrypted)
  → Used to get new access tokens
  → Invalidated on logout
```

### Why Token-Based Auth?
- **Stateless**: Server doesn't need to store session data
- **Scalable**: Works across multiple devices
- **Secure**: Short-lived access tokens limit exposure
- **Convenient**: Auto-refresh means user stays logged in

---

## 8. Pagination — Shop/Product Lists

### Algorithm
**Cursor-based pagination** for efficient loading of large lists.

### Flow
```
1. Load first page (limit: 20 items)
2. User scrolls to bottom
3. Check if more items exist (has_more flag)
4. If yes: fetch next page using last item's ID as cursor
5. Append to existing list
6. Update RecyclerView with DiffUtil
```

### Why Cursor-Based (not Offset)?
- **Consistent**: No duplicates if data changes between pages
- **Efficient**: Database uses index for cursor lookup (O(log n))
- **Real-time Safe**: New items don't shift page boundaries
- **Performance**: Avoids OFFSET which scans and discards rows

### Implementation
```java
// Supabase query with cursor
supabase.from("products")
    .select("*")
    .eq("shop_id", shopId)
    .order("created_at", Order.DESCENDING)
    .range(offset, offset + LIMIT - 1);
```

---

## 9. Payment Flow — Paystack Integration

### Algorithm
**Two-phase payment** with server-side initialization.

### Flow
```
Phase 1: Initialize (Server-side)
  1. App sends: {amount, email, order_id}
  2. Edge Function calls Paystack API with SECRET key
  3. Paystack returns: {access_code, reference}
  4. Edge Function stores reference in order record
  5. Returns access_code to app

Phase 2: Collect Payment (Client-side)
  1. App launches Paystack PaymentSheet with access_code
  2. User enters card/bank details in PaymentSheet
  3. Paystack processes payment
  4. PaymentSheet returns result to app

Phase 3: Verify (Server-side)
  1. App sends reference to Edge Function
  2. Edge Function calls Paystack verify API
  3. If successful: update order status to 'paid'
  4. Return verification result to app
```

### Why Server-Side Initialization?
- **Security**: Secret key never exposed to client
- **Integrity**: Server controls what amounts are valid
- **Auditability**: Server records all transactions
- **Webhook Support**: Paystack can notify server of payment status

---

## 10. Search & Filter — Shop/Product Discovery

### Algorithm
**Client-side filtering** for small datasets, **server-side search** for large datasets.

### Client-Side (Category Filter)
```
1. Load all products for current view
2. User taps category filter
3. Filter in-memory using Java Stream API
4. Update RecyclerView with filtered list
```

### Server-Side (Text Search)
```
1. User types in search bar
2. Debounce input (300ms delay)
3. Send query to Supabase with ILIKE filter
4. Display results
```

### Why Hybrid Approach?
- **Fast**: Client-side filters are instant (no network)
- **Scalable**: Server-side handles large datasets
- **Efficient**: Only sends search query when needed
- **UX**: Debounce prevents excessive API calls while typing

---

## 11. Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Repository** | OrderRepository | Abstracts data source, separates business logic from data |
| **MVVM** | Fragments + ViewModels | Clean separation of UI and business logic |
| **Observer** | LiveData, Realtime | Reactive UI updates when data changes |
| **Factory** | SupabaseConfig | Centralized client creation |
| **Adapter** | RecyclerView Adapters | Bridges data to UI components |
| **Singleton** | SupabaseClient, SessionManager | Single instance for app-wide access |
| **State Machine** | Order Status | Enforces valid status transitions |

---

## 12. Error Handling Strategy

### Network Errors
```
1. Check network availability
2. If offline: use Room cached data
3. If online but request fails: show toast, retry button
4. If auth error: redirect to login
```

### Validation Errors
```
1. Validate input before sending to server
2. Show inline error messages on forms
3. Disable submit button until valid
```

### Payment Errors
```
1. Paystack returns specific error codes
2. Map error codes to user-friendly messages
3. Allow retry without re-entering card details
4. Log error for debugging
```
