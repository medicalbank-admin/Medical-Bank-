# MedBank Project - Source Code & Download Guide

This guide confirms all code files, architectures, and compiled packages are verified, readable, and ready for loading and downloading.

---

## 1. Project Health & Compilation Status
- **Android Gradle Plugin (AGP)** & **Kotlin 2.0+**: Configured and synchronized.
- **Jetpack Compose & Material 3**: All UI screens render with M3 components.
- **Room Database**: Version 2 with destructive migration fallback active.
- **Firebase Firestore & Cloud Messaging**: All rules and models aligned.
- **Build Status**: Verified via `:app:assembleDebug` and `:app:testDebugUnitTest`.

---

## 2. Key Code Files & Directory Structure

### Application Core & Navigation
- `app/src/main/java/com/example/MainActivity.kt`: Main Android Activity, edge-to-edge system insets, and root composable host.
- `app/src/main/java/com/example/ui/screens/MainShell.kt`: Primary navigation shell, bottom navigation bar, and screen routing.
- `app/src/main/java/com/example/ui/MedBankViewModel.kt`: Central state holder for authentication, exams, referrals, and admin workflows.

### Referral System (Latest Feature)
- `app/src/main/java/com/example/ui/screens/profile/ReferEarnScreen.kt`: Student "Refer & Earn" screen featuring referral code display, one-tap copy, WhatsApp share, wallet balance, total earnings, and referral listing.
- `app/src/main/java/com/example/ui/screens/admin/AdminPayoutsScreen.kt`: Admin management screen for viewing pending 20% referral commissions and marking payouts as paid.
- `app/src/main/java/com/example/data/models/Models.kt`: Includes `User` (with `referralCode`, `referredBy`, `walletBalance`, `totalEarned`), `ReferralTransaction`, and `RegistrationResult`.
- `app/src/main/java/com/example/data/repository/MedBankRepository.kt`: Handles referral code generation ("MB" + 4 alphanumeric chars), code validation, 20% commission calculation on request approval, and Firestore transactions.
- `firestore.rules`: Security rules for `users/{uid}` and `referralTransactions/{id}`.

### Data Layer
- `app/src/main/java/com/example/data/local/Entities.kt`: Room entities including `UserEntity`.
- `app/src/main/java/com/example/data/local/MedBankDao.kt`: Room DAO queries and mutations.
- `app/src/main/java/com/example/data/local/MedBankDatabase.kt`: Room database definition.

---

## 3. Downloadable APK Files

The compiled debug APK is available at the following locations for download and testing:

1. **AI Studio Artifacts**:
   - `MedBank-debug.apk` (available directly in the Artifacts tab)
2. **Project Root Directory**:
   - `/MedBank-debug.apk`
   - `/MedicalBank-debug.apk`
3. **Build Outputs**:
   - `/.build-outputs/app-debug.apk`
   - `/app/build/outputs/apk/debug/app-debug.apk`

---

## 4. How to Export / Download Code
- **Download Full Project as ZIP**: In Google AI Studio, open the project menu and choose **Export Project as ZIP** to download the complete codebase.
- **Run in Android Studio**: Extract the downloaded ZIP, open the folder in Android Studio (Ladybug or newer), and Gradle will sync automatically using the included Gradle wrapper configuration.
