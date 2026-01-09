# ☕ MMUST Mobile Coffee Shop Application

## 📌 Project Overview
Students and staff at **Masinde Muliro University of Science and Technology (MMUST)** frequently experience long queues and delays when purchasing food and beverages at campus coffee shops. The existing **manual ordering and payment process** is slow, inconvenient, and does not support digital payments, despite the growing preference for mobile payment solutions.

Currently, there is **no centralized system** that allows users to view menus, place orders, or make fast and secure payments. This results in inefficiencies in service delivery and poor user experience.

To solve this problem, this project proposes a **mobile coffee shop application** that enables users to browse menus, place orders, and pay seamlessly using **M-Pesa**, Kenya’s most widely used mobile payment platform. The system aims to reduce waiting time, improve order management, and modernize the purchasing experience at MMUST coffee shops.

---

## ⚙️ Functional Requirements

### 1. User Registration & Login
- Users shall be able to create an account or log in using **email or phone number**.

### 2. Product Browsing
- Users shall view available coffee items including:
  - Product name  
  - Description  
  - Price  
  - Image  

### 3. Cart Management
- Users shall be able to:
  - Add items to the cart  
  - Update item quantities  
  - Remove items from the cart  

### 4. Checkout & Payment
- Users shall initiate payment using **M-Pesa** (STK Push or Paybill).
- The system shall confirm **payment success or failure in real time**.

### 5. Order Tracking
- Users shall view the current status of their order:
  - Pending  
  - Paid  
  - Completed  

### 6. Admin Management
- Admin users shall be able to:
  - Add, update, or remove coffee items  
  - View all orders and their payment status  

### 7. Notifications
- Users shall receive a confirmation notification after successful payment.
- Optionally, admin users may receive notifications when a new order is placed.

---

## 🛡️ Non-Functional Requirements

### 1. Security
- All communication shall use **HTTPS**.
- Sensitive payment data shall not be stored insecurely.

### 2. Performance
- Payment requests and confirmations shall be processed within **a few seconds**.

### 3. Usability
- The application shall have a **clean and intuitive user interface**.
- Navigation between products, cart, and checkout shall be simple and clear.

### 4. Reliability
- The system shall accurately reflect the correct payment status.
- The system shall handle network interruptions gracefully during payment.

### 5. Scalability
- The application shall support:
  - Adding more products  
  - Handling an increasing number of users without major changes  

### 6. Maintainability
- The system shall use a **modular code structure** to allow easy updates and future feature additions.

---

## 🚀 Conclusion
The MMUST Mobile Coffee Shop Application provides a modern, efficient, and secure solution for campus coffee shop operations. By integrating digital ordering and M-Pesa payments, the system enhances convenience, reduces queues, and significantly improves the overall user experience.
