# FCM Setup Checklist

// MANUAL STEP 1: Go to Firebase Console → Project Settings → Cloud Messaging.
// Ensure the "Firebase Cloud Messaging API (V1)" is enabled.

// MANUAL STEP 2: Download the latest google-services.json after enabling FCM
// and place it in app/google-services.json.

// MANUAL STEP 3: Deploy the Cloud Functions using the Firebase CLI:
//   firebase deploy --only functions

// MANUAL STEP 4: (iOS only — not currently supported) APNs certificate would be
// required in Firebase Console for iOS push notifications.
