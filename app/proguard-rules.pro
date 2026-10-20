# ==========================================
# 1. FIREBASE (Auth, Firestore, Storage, Messaging, AppCheck)
# ==========================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firebase Messaging Services
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-keep class * extends com.google.android.gms.internal.firebase_messaging.zzd { *; }

# ==========================================
# 2. ROOM DATABASE
# ==========================================
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ==========================================
# 3. MOSHI (JSON Parsing)
# ==========================================
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-dontwarn com.squareup.moshi.**

# ==========================================
# 4. RETROFIT & OKHTTP
# ==========================================
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ==========================================
# 5. APP MODELS (Crucial for Firestore)
# ==========================================
# Replace 'com.example' if your models are in a different package
-keep class com.example.data.models.** { *; }
-keep class com.example.data.local.** { *; }

# ==========================================
# 6. COIL (Image Loading)
# ==========================================
-keep class coil.** { *; }
-dontwarn coil.**

# ==========================================
# 7. KOTLIN COROUTINES
# ==========================================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
