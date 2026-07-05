# MMUST Coffee Shop

A modern Android application for digital ordering and M-PESA payments at Masinde Muliro University of Science and Technology coffee shops.

## About

Students and staff at MMUST face long queues when purchasing coffee. This app solves that by enabling digital ordering and mobile money payments, reducing wait times and modernizing the campus coffee shop experience.

**Project Duration:** December 22, 2025 — June 11, 2026

**Latest Release:** [Download v1.1.0 APK](https://github.com/sibby-killer/coffeshop/releases/tag/v1.1.0)

---

## Collaborators

| Name | Role |
|------|------|
| **Alfred Nyongesa** | Project Lead / Developer |
| **Ian Mwilitsa** | Developer |
| **Lovin Chebet** | Developer |
| **Bering Aura** | Developer |
| **Otieno Byrum** | Developer |
| **Cynthia Kwamboka** | Developer |

---

## Features

### Completed
- Multi-role authentication (Admin, Shop Owner, Customer)
- Shop creation and management
- Product CRUD with stock quantity tracking
- Customer browsing, cart, and checkout
- Paystack payment integration (M-Pesa, Airtel Money, Card)
- Order tracking with status updates (pending → paid → preparing → ready → completed)
- Withdrawal requests for shop owners
- Admin dashboard with user/shop management
- Forgot password functionality
- Modern coffee brown Material Design UI

### In Progress
- Push notifications for order status updates
- Product image uploads (currently using local drawables)
- Quantity decrement when customer places order

### Planned
- Loyalty program for frequent customers
- Product reviews and ratings
- Favorites / saved items
- Multiple coffee shop locations
- Order history details with item breakdown

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Platform | Native Android (Java) |
| UI | Material Design 3 |
| Backend | Supabase (PostgreSQL, Auth, Storage) |
| Payments | Paystack (M-Pesa integration via Edge Functions) |
| Networking | OkHttp |
| Local Storage | SharedPreferences |
| Build | Gradle |
| CI/CD | GitHub Actions |

---

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](documentation/ARCHITECTURE.md) | System architecture and design decisions |
| [Algorithms](documentation/ALGORITHM.md) | Data structures and algorithms used |
| [Database Schema](documentation/schema.sql) | Supabase PostgreSQL schema |
| [Migration v2](documentation/migration-v2.sql) | Adds product quantity and withdrawals table |
| [RLS Policies](documentation/fix-rls.sql) | Row Level Security setup |
| [Admin RLS](documentation/fix-admin-rls.sql) | Permissive policies for hardcoded admin |

---

## Quick Start

### Prerequisites
- Android Studio (latest stable)
- Android device or emulator (API 24+)
- Supabase account (free tier works)
- Paystack test account

### Setup
```bash
# Clone the repository
git clone https://github.com/sibby-killer/coffeshop.git
cd coffeshop

# Create local.properties with your credentials
# See below for required fields

# Open in Android Studio and sync Gradle
# Build → Run
```

### Required `local.properties` fields
```properties
sdk.dir=C\:\\Users\\<your-username>\\AppData\\Local\\Android\\Sdk
supabase.url=https://<your-project>.supabase.co
supabase.key=<your-anon-key>
paystack.key=sk_test_<your-test-key>
admin.email=<admin-email>
admin.password=<admin-password>
```

---

## CI/CD

GitHub Actions auto-builds the APK on every push to `main`. Tag a release to create a downloadable APK:

```bash
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin main && git push origin v1.2.0
```

The release page will have the `app-debug.apk` ready for download.

---

## Test Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | alfrednyongesa411@gmail.com | Admin@12345 |
| Paystack Test Card | 4084084084084081 | Exp: 12/26, CVV: 408 |

---

## Project Timeline

| Phase | Period | Deliverables |
|-------|--------|-------------|
| Planning & Research | Dec 22 — Jan 15 | Architecture docs, schema design, tech stack decisions |
| Backend Setup | Jan 16 — Feb 28 | Supabase project, RLS policies, Edge Functions |
| Core Development | Mar 1 — Apr 30 | Auth, product management, cart, checkout |
| Payment Integration | May 1 — May 20 | Paystack M-Pesa integration, order flow |
| UI Polish & Testing | May 21 — Jun 5 | Material Design UI, bug fixes, crash fixes |
| Release | Jun 6 — Jun 11 | GitHub Actions CI/CD, v1.1.0 release |

---

## Known Issues

- Payment verification requires returning to app after browser payment
- Product images are local drawables (database image upload planned)
- Screen sharing may show "content hidden" on Android 14+ (enable Developer Option "Disable screen share protections")

---

## License

This project is for educational purposes at Masinde Muliro University of Science and Technology.

---

**Built with care by the MMUST Coffee Shop team.**
