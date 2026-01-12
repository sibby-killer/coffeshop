# ✅ FINAL PRE-FLIGHT CHECKLIST

## Before Running the App

### 1. Check local.properties Format

Your file should look EXACTLY like this:

```properties
sdk.dir=C\:\\Users\\alfre\\AppData\\Local\\Android\\Sdk

supabase.url=https://uwzcpezmvbsbjnbyyvmv.supabase.co
supabase.key=YOUR_ANON_KEY_HERE
paystack.key=pk_test_YOUR_KEY_HERE
```

**Critical Points:**
- ✅ `sdk.dir` uses `C\:\\` (colon escaped with `\:` and backslashes doubled `\\`)
- ✅ NO quotes around any values
- ✅ NO spaces around `=`
- ✅ Use ANON key for Supabase (not service_role)
- ✅ Use TEST key for Paystack (pk_test_... not pk_live_...)

---

### 2. Verify Credentials Are Real

#### Supabase ANON Key Test:
```
Should start with: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
Should NOT contain: "service_role"
Get from: Settings → API → anon/public key
```

#### Paystack TEST Key Test:
```
Should start with: pk_test_
Should NOT start with: pk_live_
Get from: Dashboard (Test Mode) → Settings → API Keys
```

---

### 3. Fix Build Issues

If you get any build errors:

```bash
# Clean everything
.\gradlew clean
Remove-Item -Recurse -Force .gradle, app\build

# Rebuild
.\gradlew build

# Or in Android Studio:
# 1. File → Invalidate Caches → Invalidate and Restart
# 2. After restart: Build → Clean Project
# 3. Build → Rebuild Project
```

---

### 4. The "Invalid Key" Error

This means credentials are empty or wrong.

**Quick Fix:**
1. Open `local.properties`
2. Verify all 3 credentials are filled with REAL values
3. Clean and rebuild
4. Run again

---

### 5. The App is Running But...

#### Problem: M-PESA button doesn't work
**Check:** Is paystack.key filled with a TEST key?

#### Problem: Can't register/login
**Check:** Is supabase.key the ANON key (not service_role)?

#### Problem: App crashes on startup
**Check:** Is sdk.dir path correct with proper escaping?

---

## What's Been Changed

### ✅ Payment Button
- **Old:** "Pay with Paystack"
- **New:** "Pay with M-PESA"
- **Why:** User-friendly, hides implementation details

### ✅ Internal Names
- Still uses Paystack API internally
- M-PESA payments work through Paystack
- Users only see "M-PESA"

### ✅ SDK Path
- Auto-fixed escaping in local.properties
- Format: `C\:\\Users\\alfre\\AppData\\Local\\Android\\Sdk`

---

## Quick Test After Setup

### 1. Build Test
```
1. Open Android Studio
2. File → Sync Project with Gradle Files
3. Should complete without errors ✅
```

### 2. Run Test
```
1. Click Run ▶️
2. App installs on device ✅
3. App launches without crash ✅
```

### 3. Feature Test
```
1. Register a new account → Should work ✅
2. Browse products → Should load ✅
3. Add to cart → Should save ✅
4. Go to checkout → Should show total ✅
5. Click "Pay with M-PESA" → Browser should open ✅
```

---

## Common Mistakes to Avoid

### ❌ Wrong:
```properties
sdk.dir=C:\Users\alfre\AppData\Local\Android\Sdk
supabase.key="your_key"
paystack.key=pk_live_real_money_key
```

### ✅ Correct:
```properties
sdk.dir=C\:\\Users\\alfre\\AppData\\Local\\Android\\Sdk
supabase.key=eyJhbGc...
paystack.key=pk_test_...
```

---

## If Nothing Works

### Nuclear Option (Start Fresh):

```powershell
# 1. Delete local.properties
Remove-Item C:\Users\alfre\StudioProjects\coffeshop\local.properties

# 2. Let Android Studio create it
# Open Android Studio → File → Project Structure
# Android Studio will auto-create local.properties with SDK path

# 3. Add your credentials manually
# Edit the file and add:
supabase.url=...
supabase.key=...
paystack.key=...

# 4. Clean and rebuild
.\gradlew clean build
```

---

## Success Indicators

You know everything is working when:

1. ✅ Gradle sync completes (no red errors)
2. ✅ App builds successfully
3. ✅ App launches on device
4. ✅ You can register/login
5. ✅ Products load in Drinks tab
6. ✅ Cart works
7. ✅ Checkout shows "Pay with M-PESA" button
8. ✅ Clicking button opens browser
9. ✅ Payment page loads

---

## Final Notes

- **Payment:** M-PESA via Paystack API (users don't need to know)
- **Testing:** Use Paystack test cards for testing
- **Production:** Switch to pk_live_ key only when ready for real money
- **Security:** NEVER commit local.properties (it's in .gitignore)

---

**Everything is now fixed and ready to work!** 🎉

Just fill in your REAL credentials and run!
