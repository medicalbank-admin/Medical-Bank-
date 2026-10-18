package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.*
import com.example.data.models.*
import com.example.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class MedBankRepository(
    private val context: Context,
    private val dao: MedBankDao
) {
    private val TAG = "MedBankRepo"

    // Firebase Auth
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Firebase Firestore with custom Database ID from firebase_applet_config.xml
    private val firestore: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to default Firestore instance: ${e.message}")
            FirebaseFirestore.getInstance()
        }
    }

    // --- Helpers for JSON serialization for local caching ---
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun entitlementsToJson(map: Map<String, Entitlement>): String {
        val root = JSONObject()
        for ((k, v) in map) {
            val item = JSONObject()
            item.put("planId", v.planId)
            item.put("planName", v.planName)
            item.put("expiresAt", v.expiresAt)
            item.put("status", v.status)
            root.put(k, item)
        }
        return root.toString()
    }

    private fun jsonToEntitlements(json: String): Map<String, Entitlement> {
        if (json.isBlank()) return emptyMap()
        val result = mutableMapOf<String, Entitlement>()
        try {
            val root = JSONObject(json)
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val item = root.getJSONObject(k)
                result[k] = Entitlement(
                    planId = item.optString("planId"),
                    planName = item.optString("planName"),
                    expiresAt = item.optLong("expiresAt"),
                    status = item.optString("status", "active")
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun listToJson(list: List<String>): String {
        val array = JSONArray()
        list.forEach { array.put(it) }
        return array.toString()
    }

    private fun jsonToList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    private fun answersToJson(answers: List<StudentAnswer>): String {
        val array = JSONArray()
        answers.forEach {
            val obj = JSONObject()
            obj.put("qid", it.qid)
            obj.put("selectedIndex", it.selectedIndex)
            array.put(obj)
        }
        return array.toString()
    }

    private fun jsonToAnswers(json: String): List<StudentAnswer> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<StudentAnswer>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StudentAnswer(
                        qid = obj.getString("qid"),
                        selectedIndex = obj.getInt("selectedIndex")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun userEntityToModel(e: UserEntity): User {
        return User(
            uid = e.uid,
            role = e.role,
            fullName = e.fullName,
            cnic = e.cnic,
            email = e.email,
            phone = e.phone,
            country = e.country,
            photoUrl = e.photoUrl,
            photoBase64 = "",
            programType = e.programType,
            mbbsYear = e.mbbsYear,
            licensingPath = e.licensingPath,
            entitlements = jsonToEntitlements(e.entitlementsJson),
            createdAt = e.createdAt,
            lastLoginAt = e.lastLoginAt,
            referralCode = e.referralCode,
            referredBy = e.referredBy,
            walletBalance = e.walletBalance,
            totalEarned = e.totalEarned
        )
    }

    fun generateReferralCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "MB$suffix"
    }

    // --- STEP 2 & 3: Settings & Admin Bootstrap ---
    fun observeSettings(): Flow<AppSettings> = callbackFlow {
        val docRef = firestore.collection("settings").document("app")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Settings listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val settings = AppSettings(
                    firstAdminClaimed = snapshot.getBoolean("firstAdminClaimed") ?: false,
                    maxDevicesPerUser = snapshot.getLong("maxDevicesPerUser")?.toInt() ?: 1,
                    offlinePackExpiryDays = snapshot.getLong("offlinePackExpiryDays")?.toInt() ?: 7,
                    adminWhatsapp = snapshot.getString("adminWhatsapp") ?: "+1234567890",
                    announcementBanner = snapshot.getString("announcementBanner") ?: "",
                    updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
                trySend(settings)
            } else {
                trySend(AppSettings())
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getSettings(): AppSettings {
        return try {
            val snap = firestore.collection("settings").document("app").get().await()
            if (snap.exists()) {
                AppSettings(
                    firstAdminClaimed = snap.getBoolean("firstAdminClaimed") ?: false,
                    maxDevicesPerUser = snap.getLong("maxDevicesPerUser")?.toInt() ?: 1,
                    offlinePackExpiryDays = snap.getLong("offlinePackExpiryDays")?.toInt() ?: 7,
                    adminWhatsapp = snap.getString("adminWhatsapp") ?: "+1234567890",
                    announcementBanner = snap.getString("announcementBanner") ?: "",
                    updatedAt = snap.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            } else {
                dao.getSettings()?.let {
                    AppSettings(it.firstAdminClaimed, it.maxDevicesPerUser, it.offlinePackExpiryDays, it.adminWhatsapp, it.announcementBanner, it.updatedAt)
                } ?: AppSettings()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get settings from Firestore: ${e.message}")
            dao.getSettings()?.let {
                AppSettings(it.firstAdminClaimed, it.maxDevicesPerUser, it.offlinePackExpiryDays, it.adminWhatsapp, it.announcementBanner, it.updatedAt)
            } ?: AppSettings()
        }
    }

    suspend fun updateSettings(adminWhatsapp: String, banner: String) {
        try {
            val data = mapOf(
                "adminWhatsapp" to adminWhatsapp,
                "announcementBanner" to banner,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("settings").document("app").set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Firestore settings: ${e.message}")
        }
        dao.insertSettings(
            SettingsEntity(
                id = "app",
                firstAdminClaimed = true,
                maxDevicesPerUser = 1,
                offlinePackExpiryDays = 7,
                adminWhatsapp = adminWhatsapp,
                announcementBanner = banner,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getLegalContent(): LegalContent {
        return try {
            val snap = firestore.collection("settings").document("legal").get().await()
            if (snap.exists()) {
                LegalContent(
                    privacyPolicy = snap.getString("privacyPolicy") ?: "",
                    termsAndConditions = snap.getString("termsAndConditions") ?: "",
                    updatedAt = snap.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                )
            } else {
                LegalContent()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get legal content: ${e.message}")
            LegalContent()
        }
    }

    suspend fun saveLegalContent(privacyPolicy: String, termsAndConditions: String): Boolean {
        return try {
            val data = mapOf(
                "privacyPolicy" to privacyPolicy,
                "termsAndConditions" to termsAndConditions,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("settings").document("legal").set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save legal content: ${e.message}")
            false
        }
    }

    // --- STEP 2: Authentication ---
    suspend fun registerUser(
        fullName: String,
        cnic: String,
        email: String,
        phone: String,
        country: String,
        photoBase64: String,
        programType: String,
        mbbsYear: Int?,
        licensingPath: String?,
        password: String,
        deviceId: String,
        deviceModel: String,
        enteredReferralCode: String? = null
    ): Result<RegistrationResult> {
        val cleanCnic = cnic.trim()
        if (cleanCnic.length != 13 || !cleanCnic.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("CNIC must be exactly 13 numeric digits."))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters."))
        }

        try {
            // Check if CNIC already indexed in Firestore
            val cnicDoc = firestore.collection("cnicIndex").document(cleanCnic).get().await()
            if (cnicDoc.exists()) {
                return Result.failure(IllegalStateException("CNIC is already registered."))
            }

            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email.trim().lowercase(), password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user creation failed.")
            val uid = firebaseUser.uid

            // Determine role via Firestore transaction on settings/app
            var assignedRole = "student"
            val settingsRef = firestore.collection("settings").document("app")

            firestore.runTransaction { tx ->
                val settingsSnap = tx.get(settingsRef)
                val claimed = settingsSnap.getBoolean("firstAdminClaimed") ?: false
                if (!claimed) {
                    assignedRole = "admin"
                    // TODO: Requires custom claims — set admin role via Firestore document for now.
                    tx.set(
                        settingsRef,
                        mapOf(
                            "firstAdminClaimed" to true,
                            "maxDevicesPerUser" to 1,
                            "offlinePackExpiryDays" to 7,
                            "adminWhatsapp" to phone.ifBlank { "+1234567890" },
                            "announcementBanner" to "Welcome to MedBank! First admin account activated.",
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                } else {
                    assignedRole = "student"
                }
            }.await()

            // Check entered referral code
            var referredByUid: String? = null
            var invalidReferralCodeEntered = false
            val cleanReferral = enteredReferralCode?.trim()?.uppercase()
            if (!cleanReferral.isNullOrBlank()) {
                try {
                    val refQuery = firestore.collection("users")
                        .whereEqualTo("referralCode", cleanReferral)
                        .limit(1)
                        .get()
                        .await()
                    if (!refQuery.isEmpty) {
                        referredByUid = refQuery.documents[0].id
                    } else {
                        invalidReferralCodeEntered = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Referral code lookup failed: ${e.message}")
                    invalidReferralCodeEntered = true
                }
            }

            val newReferralCode = generateReferralCode()

            // TODO: Requires Cloud Storage — use Base64 in Firestore for now.
            val userPayload = hashMapOf<String, Any?>(
                "role" to assignedRole,
                "fullName" to fullName.trim(),
                "cnic" to cleanCnic,
                "email" to email.trim().lowercase(),
                "phone" to phone.trim(),
                "country" to country.trim(),
                "photoBase64" to photoBase64,
                "programType" to programType,
                "mbbsYear" to mbbsYear,
                "licensingPath" to licensingPath,
                "entitlements" to emptyMap<String, Any>(),
                "referralCode" to newReferralCode,
                "referredBy" to referredByUid,
                "walletBalance" to 0.0,
                "totalEarned" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp()
            )

            // Write users/{uid}
            firestore.collection("users").document(uid).set(userPayload).await()

            // Write cnicIndex/{cnic}
            firestore.collection("cnicIndex").document(cleanCnic).set(
                mapOf("uid" to uid, "email" to email.trim().lowercase())
            ).await()

            // TODO: Requires Cloud Function — not available in AI Studio Build mode.
            // bindDevice: server-side device binding with signed tokens. Writing to Firestore directly.
            val devicePayload = mapOf(
                "platform" to "Android",
                "model" to deviceModel,
                "appVersion" to "1.0",
                "firstSeen" to FieldValue.serverTimestamp(),
                "lastSeen" to FieldValue.serverTimestamp(),
                "status" to "active"
            )
            firestore.collection("users").document(uid).collection("devices").document(deviceId).set(devicePayload).await()

            // Cache locally in Room
            val userEntity = UserEntity(
                uid = uid,
                role = assignedRole,
                fullName = fullName.trim(),
                cnic = cleanCnic,
                email = email.trim().lowercase(),
                passwordHash = hashPassword(password),
                phone = phone.trim(),
                country = country.trim(),
                photoUrl = "",
                programType = programType,
                mbbsYear = mbbsYear,
                licensingPath = licensingPath,
                entitlementsJson = "{}",
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                referralCode = newReferralCode,
                referredBy = referredByUid,
                walletBalance = 0.0,
                totalEarned = 0.0
            )
            dao.insertUser(userEntity)
            dao.insertCnicIndex(CnicIndexEntity(cleanCnic, uid, email.trim().lowercase()))

            return Result.success(RegistrationResult(userEntityToModel(userEntity), invalidReferralCodeEntered))
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            return Result.failure(e)
        }
    }

    // TODO: Requires Cloud Function — not available in AI Studio Build mode.
    // resolveCnicToEmail: currently done client-side via cnicIndex read.
    suspend fun login(cnic: String, pass: String, deviceId: String, deviceModel: String): Result<User> {
        val cleanCnic = cnic.trim()
        if (cleanCnic.length != 13) {
            return Result.failure(IllegalArgumentException("CNIC must be 13 digits."))
        }

        try {
            // Step A: Resolve CNIC to Email
            val cnicDoc = firestore.collection("cnicIndex").document(cleanCnic).get().await()
            val email = if (cnicDoc.exists()) {
                cnicDoc.getString("email") ?: ""
            } else {
                dao.getCnicIndex(cleanCnic)?.email ?: ""
            }

            if (email.isBlank()) {
                return Result.failure(IllegalArgumentException("No account registered with this CNIC."))
            }

            // Step B: Authenticate via Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase sign in failed.")
            val uid = firebaseUser.uid

            // Step C: Fetch User Profile
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                return Result.failure(IllegalStateException("User record not found."))
            }

            val role = userDoc.getString("role") ?: "student"
            val fullName = userDoc.getString("fullName") ?: ""
            val phone = userDoc.getString("phone") ?: ""
            val country = userDoc.getString("country") ?: ""
            val programType = userDoc.getString("programType") ?: "mbbs"
            val mbbsYear = userDoc.getLong("mbbsYear")?.toInt()
            val licensingPath = userDoc.getString("licensingPath")
            val entitlementsRaw = userDoc.get("entitlements") as? Map<String, Any> ?: emptyMap()

            // Step D: Device Binding & Single-Device Enforcement
            // TODO: Requires Cloud Function — not available in AI Studio Build mode.
            // bindDevice: server-side device binding with signed tokens. Handled via Firestore collection check.
            val devicesRef = firestore.collection("users").document(uid).collection("devices")
            val activeDevices = devicesRef.whereEqualTo("status", "active").get().await()

            if (!activeDevices.isEmpty) {
                val matchesCurrent = activeDevices.documents.any { it.id == deviceId }
                if (!matchesCurrent) {
                    return Result.failure(IllegalStateException("Device limit reached. Only 1 device allowed. Please contact admin to reset your device."))
                }
            }

            // Update current device heartbeat
            devicesRef.document(deviceId).set(
                mapOf(
                    "platform" to "Android",
                    "model" to deviceModel,
                    "appVersion" to "1.0",
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "status" to "active"
                ),
                SetOptions.merge()
            ).await()

            // Update last login
            firestore.collection("users").document(uid).update("lastLoginAt", FieldValue.serverTimestamp())

            var referralCode = userDoc.getString("referralCode") ?: ""
            if (referralCode.isBlank()) {
                referralCode = generateReferralCode()
                firestore.collection("users").document(uid).update("referralCode", referralCode)
            }
            val referredBy = userDoc.getString("referredBy")
            val walletBalance = userDoc.getDouble("walletBalance") ?: (userDoc.getLong("walletBalance")?.toDouble() ?: 0.0)
            val totalEarned = userDoc.getDouble("totalEarned") ?: (userDoc.getLong("totalEarned")?.toDouble() ?: 0.0)

            // Map entitlements
            val parsedEntitlements = mutableMapOf<String, Entitlement>()
            entitlementsRaw.forEach { (scopeId, value) ->
                if (value is Map<*, *>) {
                    parsedEntitlements[scopeId] = Entitlement(
                        planId = value["planId"] as? String ?: "",
                        planName = value["planName"] as? String ?: "Active Pass",
                        expiresAt = (value["expiresAt"] as? Number)?.toLong() ?: 0L,
                        status = value["status"] as? String ?: "active"
                    )
                }
            }

            // Sync with local Room database
            val userEntity = UserEntity(
                uid = uid,
                role = role,
                fullName = fullName,
                cnic = cleanCnic,
                email = email,
                passwordHash = hashPassword(pass),
                phone = phone,
                country = country,
                photoUrl = "",
                programType = programType,
                mbbsYear = mbbsYear,
                licensingPath = licensingPath,
                entitlementsJson = entitlementsToJson(parsedEntitlements),
                createdAt = userDoc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                referralCode = referralCode,
                referredBy = referredBy,
                walletBalance = walletBalance,
                totalEarned = totalEarned
            )
            dao.insertUser(userEntity)

            return Result.success(userEntityToModel(userEntity))
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            return Result.failure(e)
        }
    }

    suspend fun forgotPassword(cnic: String): Result<String> {
        val cleanCnic = cnic.trim()
        try {
            val cnicDoc = firestore.collection("cnicIndex").document(cleanCnic).get().await()
            val email = if (cnicDoc.exists()) {
                cnicDoc.getString("email") ?: ""
            } else {
                dao.getCnicIndex(cleanCnic)?.email ?: ""
            }

            if (email.isBlank()) {
                return Result.failure(IllegalArgumentException("CNIC not found in system."))
            }

            auth.sendPasswordResetEmail(email).await()
            return Result.success("Password reset instructions sent to $email.")
        } catch (e: Exception) {
            Log.e(TAG, "Forgot password error: ${e.message}", e)
            return Result.failure(e)
        }
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val docRef = firestore.collection("users").document(uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "User listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val entitlementsRaw = snapshot.get("entitlements") as? Map<String, Any> ?: emptyMap()
                val parsed = mutableMapOf<String, Entitlement>()
                entitlementsRaw.forEach { (scopeId, value) ->
                    if (value is Map<*, *>) {
                        parsed[scopeId] = Entitlement(
                            planId = value["planId"] as? String ?: "",
                            planName = value["planName"] as? String ?: "Active Pass",
                            expiresAt = (value["expiresAt"] as? Number)?.toLong() ?: 0L,
                            status = value["status"] as? String ?: "active"
                        )
                    }
                }

                val u = User(
                    uid = snapshot.id,
                    role = snapshot.getString("role") ?: "student",
                    fullName = snapshot.getString("fullName") ?: "",
                    cnic = snapshot.getString("cnic") ?: "",
                    email = snapshot.getString("email") ?: "",
                    phone = snapshot.getString("phone") ?: "",
                    country = snapshot.getString("country") ?: "",
                    photoUrl = snapshot.getString("photoUrl") ?: "",
                    photoBase64 = snapshot.getString("photoBase64") ?: "",
                    programType = snapshot.getString("programType") ?: "mbbs",
                    mbbsYear = snapshot.getLong("mbbsYear")?.toInt(),
                    licensingPath = snapshot.getString("licensingPath"),
                    entitlements = parsed,
                    fcmToken = snapshot.getString("fcmToken") ?: "",
                    fcmTokenUpdatedAt = snapshot.getLong("fcmTokenUpdatedAt") ?: 0L,
                    subscribedTopics = (snapshot.get("subscribedTopics") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    notificationOptIn = snapshot.getBoolean("notificationOptIn") ?: true,
                    createdAt = snapshot.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    lastLoginAt = snapshot.getTimestamp("lastLoginAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    referralCode = snapshot.getString("referralCode") ?: "",
                    referredBy = snapshot.getString("referredBy"),
                    walletBalance = snapshot.getDouble("walletBalance") ?: (snapshot.getLong("walletBalance")?.toDouble() ?: 0.0),
                    totalEarned = snapshot.getDouble("totalEarned") ?: (snapshot.getLong("totalEarned")?.toDouble() ?: 0.0)
                )
                trySend(u)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAllUsers(): Flow<List<User>> = callbackFlow {
        val colRef = firestore.collection("users")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val list = snapshot?.documents?.map { d ->
                User(
                    uid = d.id,
                    role = d.getString("role") ?: "student",
                    fullName = d.getString("fullName") ?: "",
                    cnic = d.getString("cnic") ?: "",
                    email = d.getString("email") ?: "",
                    phone = d.getString("phone") ?: "",
                    country = d.getString("country") ?: "",
                    programType = d.getString("programType") ?: "mbbs",
                    mbbsYear = d.getLong("mbbsYear")?.toInt(),
                    licensingPath = d.getString("licensingPath"),
                    referralCode = d.getString("referralCode") ?: "",
                    referredBy = d.getString("referredBy"),
                    walletBalance = d.getDouble("walletBalance") ?: (d.getLong("walletBalance")?.toDouble() ?: 0.0),
                    totalEarned = d.getDouble("totalEarned") ?: (d.getLong("totalEarned")?.toDouble() ?: 0.0),
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeReferralsForUser(uid: String): Flow<List<User>> = callbackFlow {
        val colRef = firestore.collection("users").whereEqualTo("referredBy", uid)
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Referrals listener error: ${error.message}")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.map { d ->
                User(
                    uid = d.id,
                    role = d.getString("role") ?: "student",
                    fullName = d.getString("fullName") ?: "Candidate",
                    email = d.getString("email") ?: "",
                    phone = d.getString("phone") ?: "",
                    cnic = d.getString("cnic") ?: "",
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    walletBalance = d.getDouble("walletBalance") ?: (d.getLong("walletBalance")?.toDouble() ?: 0.0)
                )
            }?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }


    suspend fun resetUserDevice(uid: String) {
        try {
            val devices = firestore.collection("users").document(uid).collection("devices").get().await()
            for (d in devices.documents) {
                d.reference.update("status", "revoked").await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Device reset error: ${e.message}")
        }
        dao.revokeAllDevicesForUser(uid)
    }

    suspend fun promoteUserToAdmin(uid: String) {
        try {
            firestore.collection("users").document(uid).update("role", "admin").await()
        } catch (e: Exception) {
            Log.e(TAG, "Promote user error: ${e.message}")
        }
        val user = dao.getUserByUid(uid) ?: return
        dao.updateUser(user.copy(role = "admin"))
    }

    // --- STEP 3: Content Hierarchy (Programs, Years, Pathways, Subjects) ---
    fun observeAllPrograms(): Flow<List<Program>> = callbackFlow {
        val ref = firestore.collection("programs").orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                Program(d.id, d.getString("name") ?: "", d.getString("type") ?: "", d.getLong("order")?.toInt() ?: 0, d.getBoolean("isActive") ?: true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeActiveYears(): Flow<List<MbbsYear>> = callbackFlow {
        val ref = firestore.collection("mbbsYears").whereEqualTo("isActive", true).orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                MbbsYear(d.id, d.getString("programId") ?: "mbbs", d.getString("name") ?: "", d.getLong("order")?.toInt() ?: 1, d.getLong("order")?.toInt() ?: 0, true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAllYears(): Flow<List<MbbsYear>> = callbackFlow {
        val ref = firestore.collection("mbbsYears").orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                MbbsYear(d.id, d.getString("programId") ?: "mbbs", d.getString("name") ?: "", d.getLong("order")?.toInt() ?: 1, d.getLong("order")?.toInt() ?: 0, d.getBoolean("isActive") ?: true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeActivePathways(): Flow<List<LicensingPathway>> = callbackFlow {
        val ref = firestore.collection("licensingPathways").whereEqualTo("isActive", true).orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                LicensingPathway(d.id, d.getString("programId") ?: "licensing", d.getString("name") ?: "", d.getString("code") ?: "", d.getLong("order")?.toInt() ?: 0, true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAllPathways(): Flow<List<LicensingPathway>> = callbackFlow {
        val ref = firestore.collection("licensingPathways").orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                LicensingPathway(d.id, d.getString("programId") ?: "licensing", d.getString("name") ?: "", d.getString("code") ?: "", d.getLong("order")?.toInt() ?: 0, d.getBoolean("isActive") ?: true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeSubjectsByParent(parentType: String, parentId: String): Flow<List<Subject>> = callbackFlow {
        val ref = firestore.collection("subjects")
            .whereEqualTo("parentType", parentType)
            .whereEqualTo("parentId", parentId)
            .whereEqualTo("isActive", true)
            .orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                Subject(d.id, parentType, parentId, d.getString("name") ?: "", d.getLong("order")?.toInt() ?: 0, true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAllSubjects(): Flow<List<Subject>> = callbackFlow {
        val ref = firestore.collection("subjects").orderBy("name")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                Subject(d.id, d.getString("parentType") ?: "", d.getString("parentId") ?: "", d.getString("name") ?: "", d.getLong("order")?.toInt() ?: 0, d.getBoolean("isActive") ?: true)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveProgram(program: Program) {
        val data = mapOf(
            "name" to program.name,
            "type" to program.type,
            "order" to program.order,
            "isActive" to program.isActive
        )
        firestore.collection("programs").document(program.id).set(data).await()
        dao.insertProgram(ProgramEntity(program.id, program.name, program.type, program.order, program.isActive))
    }

    suspend fun saveYear(year: MbbsYear) {
        val data = mapOf(
            "programId" to year.programId,
            "name" to year.name,
            "order" to year.yearNumber,
            "isActive" to year.isActive
        )
        firestore.collection("mbbsYears").document(year.id).set(data).await()
        dao.insertYear(MbbsYearEntity(year.id, year.programId, year.name, year.yearNumber, year.order, year.isActive))
    }

    suspend fun savePathway(pathway: LicensingPathway) {
        val data = mapOf(
            "programId" to pathway.programId,
            "name" to pathway.name,
            "code" to pathway.code,
            "order" to pathway.order,
            "isActive" to pathway.isActive
        )
        firestore.collection("licensingPathways").document(pathway.id).set(data).await()
        dao.insertPathway(LicensingPathwayEntity(pathway.id, pathway.programId, pathway.name, pathway.code, pathway.order, pathway.isActive))
    }

    suspend fun saveSubject(subject: Subject) {
        val data = mapOf(
            "parentType" to subject.parentType,
            "parentId" to subject.parentId,
            "name" to subject.name,
            "order" to subject.order,
            "isActive" to subject.isActive
        )
        firestore.collection("subjects").document(subject.id).set(data).await()
        dao.insertSubject(SubjectEntity(subject.id, subject.parentType, subject.parentId, subject.name, subject.order, subject.isActive))
    }

    // Helper: Admin one-click scaffold for standard MBBS (Years 1-6) and Licensing Pathways
    suspend fun scaffoldDefaultStructureIfRequested() {
        val batch = firestore.batch()

        // Programs
        val mbbsProg = firestore.collection("programs").document("mbbs")
        batch.set(mbbsProg, mapOf("name" to "MBBS", "type" to "mbbs", "order" to 1, "isActive" to true))
        val licensingProg = firestore.collection("programs").document("licensing")
        batch.set(licensingProg, mapOf("name" to "Licensing Exams", "type" to "licensing", "order" to 2, "isActive" to true))

        // MBBS Years 1 to 6
        for (i in 1..6) {
            val yRef = firestore.collection("mbbsYears").document("year_$i")
            batch.set(yRef, mapOf("programId" to "mbbs", "name" to "Year $i", "order" to i, "isActive" to true))
            dao.insertYear(MbbsYearEntity("year_$i", "mbbs", "Year $i", i, i, true))
        }

        // Licensing Pathways: NRE, FCPS, USMLE, PLAB, DHA, SMLE
        val pathways = listOf(
            Triple("usmle", "USMLE (United States Medical Licensing Exam)", "USMLE"),
            Triple("nre", "NRE (National Registration Examination)", "NRE"),
            Triple("fcps", "FCPS Part 1 & 2", "FCPS"),
            Triple("plab", "PLAB / UKMLA", "PLAB"),
            Triple("dha", "DHA (Dubai Health Authority)", "DHA"),
            Triple("smle", "SMLE (Saudi Medical Licensing Exam)", "SMLE")
        )
        pathways.forEachIndexed { idx, (id, name, code) ->
            val pRef = firestore.collection("licensingPathways").document(id)
            batch.set(pRef, mapOf("programId" to "licensing", "name" to name, "code" to code, "order" to idx + 1, "isActive" to true))
            dao.insertPathway(LicensingPathwayEntity(id, "licensing", name, code, idx + 1, true))
        }

        batch.commit().await()
    }

    // --- Exams & Questions ---
    fun observePublishedExamsBySubject(subjectId: String): Flow<List<Exam>> = callbackFlow {
        val ref = firestore.collection("exams")
            .whereEqualTo("subjectId", subjectId)
            .whereEqualTo("isPublished", true)
            .orderBy("createdAt")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d -> firestoreDocToExam(d) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAllExams(): Flow<List<Exam>> = callbackFlow {
        val ref = firestore.collection("exams").orderBy("createdAt")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d -> firestoreDocToExam(d) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun getExamById(id: String): Exam? {
        val doc = firestore.collection("exams").document(id).get().await()
        return if (doc.exists()) firestoreDocToExam(doc) else dao.getExamById(id)?.let {
            Exam(it.id, it.title, it.subjectId, it.parentType, it.parentId, it.durationMin, it.totalMarks, it.negativeMarking, it.maxAttempts, it.offlineEnabled, it.isPublished, jsonToList(it.accessPlanIdsJson), it.questionCount, it.createdAt, it.updatedAt)
        }
    }

    private fun firestoreDocToExam(d: com.google.firebase.firestore.DocumentSnapshot): Exam? {
        if (!d.exists()) return null
        return Exam(
            id = d.id,
            title = d.getString("title") ?: "",
            subjectId = d.getString("subjectId") ?: "",
            parentType = d.getString("parentType") ?: "",
            parentId = d.getString("parentId") ?: "",
            durationMin = d.getLong("durationMin")?.toInt() ?: 60,
            totalMarks = d.getDouble("totalMarks") ?: (d.getLong("totalMarks")?.toDouble() ?: 100.0),
            negativeMarking = d.getDouble("negativeMarking") ?: 0.25,
            maxAttempts = d.getLong("maxAttempts")?.toInt() ?: 1,
            offlineEnabled = d.getBoolean("offlineEnabled") ?: false,
            isPublished = d.getBoolean("isPublished") ?: true,
            accessPlanIds = (d.get("accessPlanIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            questionCount = d.getLong("questionCount")?.toInt() ?: 0,
            createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAt = d.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    suspend fun saveExam(exam: Exam) {
        val data = hashMapOf(
            "title" to exam.title,
            "subjectId" to exam.subjectId,
            "parentType" to exam.parentType,
            "parentId" to exam.parentId,
            "durationMin" to exam.durationMin,
            "totalMarks" to exam.totalMarks,
            "negativeMarking" to exam.negativeMarking,
            "maxAttempts" to exam.maxAttempts,
            "offlineEnabled" to exam.offlineEnabled,
            "isPublished" to exam.isPublished,
            "accessPlanIds" to exam.accessPlanIds,
            "questionCount" to exam.questionCount,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("exams").document(exam.id).set(data).await()

        // TODO: Requires Cloud Function — not available in AI Studio Build mode.
        // onNewExamPublished: sends FCM notifications to students in scope.

        dao.insertExam(
            ExamEntity(
                id = exam.id,
                title = exam.title,
                subjectId = exam.subjectId,
                parentType = exam.parentType,
                parentId = exam.parentId,
                durationMin = exam.durationMin,
                totalMarks = exam.totalMarks,
                negativeMarking = exam.negativeMarking,
                maxAttempts = exam.maxAttempts,
                offlineEnabled = exam.offlineEnabled,
                isPublished = exam.isPublished,
                accessPlanIdsJson = listToJson(exam.accessPlanIds),
                questionCount = exam.questionCount,
                createdAt = exam.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteExam(examId: String) {
        try {
            firestore.collection("exams").document(examId).delete().await()
        } catch (_: Exception) {}
        val exam = dao.getExamById(examId) ?: return
        dao.deleteExam(exam)
    }

    suspend fun getQuestionsForExam(examId: String): List<Question> {
        return try {
            val snaps = firestore.collection("exams").document(examId)
                .collection("questions").orderBy("order").get().await()

            if (!snaps.isEmpty) {
                snaps.documents.map { d ->
                    Question(
                        id = d.id,
                        examId = examId,
                        order = d.getLong("order")?.toInt() ?: 1,
                        text = d.getString("text") ?: "",
                        options = (d.get("options") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        difficulty = d.getString("difficulty") ?: "medium",
                        tags = (d.get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    )
                }
            } else {
                dao.getQuestionsByExam(examId).map {
                    Question(it.id, it.examId, it.orderIndex, it.text, jsonToList(it.optionsJson), it.difficulty, jsonToList(it.tagsJson))
                }
            }
        } catch (e: Exception) {
            dao.getQuestionsByExam(examId).map {
                Question(it.id, it.examId, it.orderIndex, it.text, jsonToList(it.optionsJson), it.difficulty, jsonToList(it.tagsJson))
            }
        }
    }

    suspend fun getAnswersForExam(examId: String): List<PrivateAnswer> {
        return try {
            val snaps = firestore.collection("examPrivate").document(examId)
                .collection("answers").get().await()

            if (!snaps.isEmpty) {
                snaps.documents.map { d ->
                    PrivateAnswer(
                        id = d.id,
                        examId = examId,
                        qid = d.id,
                        correctIndex = d.getLong("correctIndex")?.toInt() ?: 0,
                        explanation = d.getString("explanation") ?: ""
                    )
                }
            } else {
                dao.getAnswersByExam(examId).map {
                    PrivateAnswer(it.id, it.examId, it.qid, it.correctIndex, it.explanation)
                }
            }
        } catch (e: Exception) {
            dao.getAnswersByExam(examId).map {
                PrivateAnswer(it.id, it.examId, it.qid, it.correctIndex, it.explanation)
            }
        }
    }

    // --- STEP 3: Server-Grade Exam Submission ---
    // TODO: Requires Cloud Function — not available in AI Studio Build mode.
    // submitExam: server-side grading with negative marking. Implemented client-side with cloud persistence for now.
    suspend fun submitExam(
        uid: String,
        examId: String,
        studentAnswers: List<StudentAnswer>,
        startedAt: Long,
        deviceId: String,
        cachedExam: Exam? = null,
        cachedQuestions: List<Question>? = null
    ): Result<ExamResult> {
        val exam = cachedExam ?: getExamById(examId)
            ?: return Result.failure(IllegalStateException("Exam not found."))

        val pastResults = getPastResultsForExam(uid, examId)
        val attemptNumber = pastResults.size + 1
        if (attemptNumber > exam.maxAttempts) {
            return Result.failure(IllegalStateException("Maximum allowed attempts (${exam.maxAttempts}) reached."))
        }

        val privateAnswers = getAnswersForExam(examId).associateBy { it.qid }
        val questions = cachedQuestions ?: getQuestionsForExam(examId)
        val qCount = questions.size.coerceAtLeast(1)
        val marksPerQuestion = exam.totalMarks / qCount

        var correctCount = 0
        var wrongCount = 0
        var skippedCount = 0
        var totalScore = 0.0

        studentAnswers.forEach { sa ->
            val correct = privateAnswers[sa.qid]
            if (sa.selectedIndex == -1) {
                skippedCount++
            } else if (correct != null && sa.selectedIndex == correct.correctIndex) {
                correctCount++
                totalScore += marksPerQuestion
            } else {
                wrongCount++
                totalScore -= exam.negativeMarking
            }
        }

        val answeredQids = studentAnswers.map { it.qid }.toSet()
        questions.forEach { q ->
            if (!answeredQids.contains(q.id)) skippedCount++
        }

        val finalScore = (totalScore.coerceAtLeast(0.0) * 100.0) / 100.0
        val resultId = UUID.randomUUID().toString()

        val resultPayload = hashMapOf(
            "uid" to uid,
            "examId" to examId,
            "examTitle" to exam.title,
            "score" to finalScore,
            "totalMarks" to exam.totalMarks.toInt(),
            "correct" to correctCount,
            "wrong" to wrongCount,
            "skipped" to skippedCount,
            "answers" to studentAnswers.map { mapOf("qid" to it.qid, "selectedIndex" to it.selectedIndex) },
            "startedAt" to FieldValue.serverTimestamp(),
            "submittedAt" to FieldValue.serverTimestamp(),
            "attemptNumber" to attemptNumber,
            "syncStatus" to "synced",
            "deviceId" to deviceId
        )

        try {
            val batch = firestore.batch()
            val resultRef = firestore.collection("results").document(resultId)
            batch.set(resultRef, resultPayload)
            
            val completedExamsRef = firestore.collection("users").document(uid)
                .collection("completedExams").document(examId)
            batch.set(completedExamsRef, mapOf(
                "examId" to examId,
                "resultId" to resultId,
                "completedAt" to FieldValue.serverTimestamp(),
                "attemptNumber" to attemptNumber
            ))
            
            batch.commit().await()
            Log.d("MEDBANK_SUBMIT", "completedExams written: uid=$uid examId=$examId")
        } catch (e: Exception) {
            Log.e("MEDBANK_SUBMIT", "Batch write failed: ${e.message}")
        }

        // Cache locally
        dao.insertResult(
            ExamResultEntity(
                id = resultId,
                uid = uid,
                examId = examId,
                examTitle = exam.title,
                score = finalScore,
                totalMarks = exam.totalMarks,
                correctCount = correctCount,
                wrongCount = wrongCount,
                skippedCount = skippedCount,
                answersJson = answersToJson(studentAnswers),
                startedAt = startedAt,
                submittedAt = System.currentTimeMillis(),
                attemptNumber = attemptNumber,
                syncStatus = "synced",
                deviceId = deviceId
            )
        )

        return Result.success(
            ExamResult(
                id = resultId,
                uid = uid,
                examId = examId,
                examTitle = exam.title,
                score = finalScore,
                totalMarks = exam.totalMarks,
                correctCount = correctCount,
                wrongCount = wrongCount,
                skippedCount = skippedCount,
                answers = studentAnswers,
                startedAt = startedAt,
                submittedAt = System.currentTimeMillis(),
                attemptNumber = attemptNumber,
                syncStatus = "synced",
                deviceId = deviceId
            )
        )
    }

    fun observeResultsByUser(uid: String): Flow<List<ExamResult>> = callbackFlow {
        val ref = firestore.collection("results").whereEqualTo("uid", uid).orderBy("submittedAt")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                val answersRaw = (d.get("answers") as? List<*>)?.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        StudentAnswer(
                            qid = item["qid"]?.toString() ?: "",
                            selectedIndex = (item["selectedIndex"] as? Number)?.toInt() ?: -1
                        )
                    } else null
                } ?: emptyList()

                ExamResult(
                    id = d.id,
                    uid = uid,
                    examId = d.getString("examId") ?: "",
                    examTitle = d.getString("examTitle") ?: "",
                    score = d.getDouble("score") ?: 0.0,
                    totalMarks = (d.getLong("totalMarks")?.toDouble()) ?: 100.0,
                    correctCount = d.getLong("correct")?.toInt() ?: 0,
                    wrongCount = d.getLong("wrong")?.toInt() ?: 0,
                    skippedCount = d.getLong("skipped")?.toInt() ?: 0,
                    answers = answersRaw,
                    attemptNumber = d.getLong("attemptNumber")?.toInt() ?: 1,
                    submittedAt = d.getTimestamp("submittedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAllResults(): Flow<List<ExamResult>> = callbackFlow {
        val ref = firestore.collection("results").orderBy("submittedAt")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d ->
                val answersRaw = (d.get("answers") as? List<*>)?.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        StudentAnswer(
                            qid = item["qid"]?.toString() ?: "",
                            selectedIndex = (item["selectedIndex"] as? Number)?.toInt() ?: -1
                        )
                    } else null
                } ?: emptyList()

                ExamResult(
                    id = d.id,
                    uid = d.getString("uid") ?: "",
                    examId = d.getString("examId") ?: "",
                    examTitle = d.getString("examTitle") ?: "",
                    score = d.getDouble("score") ?: 0.0,
                    totalMarks = (d.getLong("totalMarks")?.toDouble()) ?: 100.0,
                    correctCount = d.getLong("correct")?.toInt() ?: 0,
                    wrongCount = d.getLong("wrong")?.toInt() ?: 0,
                    skippedCount = d.getLong("skipped")?.toInt() ?: 0,
                    answers = answersRaw,
                    attemptNumber = d.getLong("attemptNumber")?.toInt() ?: 1,
                    submittedAt = d.getTimestamp("submittedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun getResultById(resultId: String): ExamResult? {
        val it = dao.getResultById(resultId) ?: return null
        return ExamResult(
            id = it.id,
            uid = it.uid,
            examId = it.examId,
            examTitle = it.examTitle,
            score = it.score,
            totalMarks = it.totalMarks,
            correctCount = it.correctCount,
            wrongCount = it.wrongCount,
            skippedCount = it.skippedCount,
            answers = jsonToAnswers(it.answersJson),
            startedAt = it.startedAt,
            submittedAt = it.submittedAt,
            attemptNumber = it.attemptNumber,
            syncStatus = it.syncStatus,
            deviceId = it.deviceId
        )
    }

    suspend fun getPastResultsForExam(uid: String, examId: String): List<ExamResult> {
        return dao.getResultsForExamByUser(uid, examId).map {
            ExamResult(
                id = it.id,
                uid = it.uid,
                examId = it.examId,
                examTitle = it.examTitle,
                score = it.score,
                totalMarks = it.totalMarks,
                correctCount = it.correctCount,
                wrongCount = it.wrongCount,
                skippedCount = it.skippedCount,
                answers = jsonToAnswers(it.answersJson),
                startedAt = it.startedAt,
                submittedAt = it.submittedAt,
                attemptNumber = it.attemptNumber,
                syncStatus = it.syncStatus,
                deviceId = it.deviceId
            )
        }
    }

    // --- STEP 3: Access Requests & Membership Plans ---
    fun observeAllAccessRequests(): Flow<List<AccessRequest>> = callbackFlow {
        val ref = firestore.collection("accessRequests").orderBy("createdAt")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                AccessRequest(
                    id = d.id,
                    uid = d.getString("uid") ?: "",
                    studentName = d.getString("studentName") ?: "",
                    cnic = d.getString("cnic") ?: "",
                    scopeType = d.getString("scopeType") ?: "",
                    scopeId = d.getString("scopeId") ?: "",
                    planId = d.getString("planId") ?: "",
                    planName = d.getString("planName") ?: "",
                    paymentProofUrl = d.getString("paymentProofUrl") ?: "",
                    paymentProofBase64 = d.getString("paymentProofBase64") ?: "",
                    status = d.getString("status") ?: "pending",
                    adminNote = d.getString("adminNote") ?: "",
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observePendingRequestsForUser(uid: String): Flow<List<AccessRequest>> = callbackFlow {
        val ref = firestore.collection("accessRequests")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "pending")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.map { d ->
                AccessRequest(
                    id = d.id,
                    uid = d.getString("uid") ?: "",
                    studentName = d.getString("studentName") ?: "",
                    cnic = d.getString("cnic") ?: "",
                    scopeType = d.getString("scopeType") ?: "",
                    scopeId = d.getString("scopeId") ?: "",
                    planId = d.getString("planId") ?: "",
                    planName = d.getString("planName") ?: "",
                    paymentProofUrl = d.getString("paymentProofUrl") ?: "",
                    status = d.getString("status") ?: "pending"
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun createAccessRequest(
        uid: String,
        studentName: String,
        cnic: String,
        scopeType: String,
        scopeId: String,
        planId: String,
        paymentProofBase64: String,
        whatsappSent: Boolean,
        planName: String = "",
        paymentProofUrl: String = ""
    ): AccessRequest {
        val reqId = UUID.randomUUID().toString()
        val data = hashMapOf(
            "uid" to uid,
            "studentName" to studentName,
            "cnic" to cnic,
            "scopeType" to scopeType,
            "scopeId" to scopeId,
            "planId" to planId,
            "planName" to planName,
            "paymentProofUrl" to paymentProofUrl,
            "paymentProofBase64" to paymentProofBase64,
            "whatsappSent" to whatsappSent,
            "status" to "pending",
            "adminNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "approvedAt" to null
        )

        try {
            firestore.collection("accessRequests").document(reqId).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating access request: ${e.message}")
        }

        dao.insertAccessRequest(
            AccessRequestEntity(
                id = reqId,
                uid = uid,
                studentName = studentName,
                cnic = cnic,
                scopeType = scopeType,
                scopeId = scopeId,
                planId = planId,
                planName = planName.ifBlank { "Exam Access Pass" },
                paymentProofUrl = paymentProofUrl.ifBlank { "payment_receipt.jpg" },
                whatsappSent = whatsappSent,
                status = "pending",
                adminNote = "",
                createdAt = System.currentTimeMillis(),
                approvedAt = null
            )
        )

        return AccessRequest(
            id = reqId,
            uid = uid,
            studentName = studentName,
            cnic = cnic,
            scopeType = scopeType,
            scopeId = scopeId,
            planId = planId,
            planName = planName,
            paymentProofUrl = paymentProofUrl,
            paymentProofBase64 = paymentProofBase64,
            whatsappSent = whatsappSent,
            status = "pending"
        )
    }

    suspend fun uploadPaymentProofAndCreateRequest(
        uid: String,
        studentName: String,
        cnic: String,
        scopeType: String,
        scopeId: String,
        planId: String,
        planName: String,
        fileUri: android.net.Uri?,
        whatsappSent: Boolean
    ): AccessRequest {
        val reqId = UUID.randomUUID().toString()
        var paymentProofDownloadUrl = ""

        if (fileUri != null) {
            try {
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                    .reference.child("paymentProofs/$uid/$reqId")
                storageRef.putFile(fileUri).await()
                paymentProofDownloadUrl = storageRef.downloadUrl.await().toString()
                Log.d("MEDBANK_UPLOAD", "Uploaded payment proof to $paymentProofDownloadUrl")
            } catch (e: Exception) {
                Log.e("MEDBANK_UPLOAD", "Failed uploading proof to storage: ${e.message}")
                paymentProofDownloadUrl = fileUri.toString()
            }
        }

        val data = hashMapOf(
            "uid" to uid,
            "studentName" to studentName,
            "cnic" to cnic,
            "scopeType" to scopeType,
            "scopeId" to scopeId,
            "planId" to planId,
            "planName" to planName,
            "paymentProofUrl" to paymentProofDownloadUrl,
            "whatsappSent" to whatsappSent,
            "status" to "pending",
            "adminNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "approvedAt" to null
        )

        try {
            firestore.collection("accessRequests").document(reqId).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating access request: ${e.message}")
        }

        dao.insertAccessRequest(
            AccessRequestEntity(
                id = reqId,
                uid = uid,
                studentName = studentName,
                cnic = cnic,
                scopeType = scopeType,
                scopeId = scopeId,
                planId = planId,
                planName = planName,
                paymentProofUrl = paymentProofDownloadUrl,
                whatsappSent = whatsappSent,
                status = "pending",
                adminNote = "",
                createdAt = System.currentTimeMillis(),
                approvedAt = null
            )
        )

        return AccessRequest(
            id = reqId,
            uid = uid,
            studentName = studentName,
            cnic = cnic,
            scopeType = scopeType,
            scopeId = scopeId,
            planId = planId,
            planName = planName,
            paymentProofUrl = paymentProofDownloadUrl,
            whatsappSent = whatsappSent,
            status = "pending"
        )
    }

    suspend fun updateRequestStatus(
        req: AccessRequest,
        isApproved: Boolean,
        adminNote: String,
        overridePlan: MembershipPlan? = null
    ) {
        val newStatus = if (isApproved) "approved" else "rejected"
        val approvedAtTimestamp = if (isApproved) FieldValue.serverTimestamp() else null
        val grantedPlanId = overridePlan?.id ?: req.planId
        val grantedPlanName = overridePlan?.name ?: req.planName.ifBlank { "Active Pass" }
        val durationDays = overridePlan?.durationDays ?: 30

        try {
            val updateData = mutableMapOf<String, Any?>(
                "status" to newStatus,
                "adminNote" to adminNote,
                "approvedAt" to approvedAtTimestamp
            )
            if (overridePlan != null) {
                updateData["planId"] = grantedPlanId
                updateData["planName"] = grantedPlanName
            }
            firestore.collection("accessRequests").document(req.id).update(updateData).await()

            if (isApproved) {
                val expiresAt = System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000)
                val entData = mapOf(
                    "planId" to grantedPlanId,
                    "planName" to grantedPlanName,
                    "expiresAt" to expiresAt,
                    "status" to "active"
                )
                firestore.collection("users").document(req.uid).update(
                    "entitlements.${req.scopeId}", entData
                ).await()

                // Check if student was referred by another user
                try {
                    val studentDoc = firestore.collection("users").document(req.uid).get().await()
                    val referrerUid = studentDoc.getString("referredBy")
                    if (!referrerUid.isNullOrBlank()) {
                        // Determine plan price
                        var planPrice = overridePlan?.price ?: 0.0
                        if (planPrice <= 0.0) {
                            val planSnap = firestore.collection("membershipPlans").document(grantedPlanId).get().await()
                            if (planSnap.exists()) {
                                planPrice = planSnap.getDouble("price") ?: (planSnap.getLong("price")?.toDouble() ?: 0.0)
                            }
                        }
                        if (planPrice <= 0.0) {
                            planPrice = 2500.0 // Default PKR price if plan has no price set
                        }

                        val commissionAmount = (planPrice * 0.20)
                        val txId = UUID.randomUUID().toString()

                        // Fetch referrer name for quick display
                        val referrerSnap = firestore.collection("users").document(referrerUid).get().await()
                        val referrerName = referrerSnap.getString("fullName") ?: "Referrer"

                        val txData = hashMapOf<String, Any?>(
                            "referrerUid" to referrerUid,
                            "referrerName" to referrerName,
                            "referredUid" to req.uid,
                            "referredName" to (studentDoc.getString("fullName") ?: req.studentName),
                            "planName" to grantedPlanName,
                            "amountPaid" to planPrice,
                            "commissionAmount" to commissionAmount,
                            "status" to "pending",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "paidAt" to null
                        )
                        firestore.collection("referralTransactions").document(txId).set(txData).await()

                        // Update referrer walletBalance and totalEarned
                        firestore.collection("users").document(referrerUid).update(
                            "walletBalance", FieldValue.increment(commissionAmount),
                            "totalEarned", FieldValue.increment(commissionAmount)
                        ).await()

                        // Send notification to referrer
                        try {
                            val commStr = if (commissionAmount % 1.0 == 0.0) commissionAmount.toInt().toString() else String.format("%.2f", commissionAmount)
                            NotificationHelper.showNotification(
                                context = context,
                                title = "Referral Bonus!",
                                body = "You earned PKR $commStr from a referral!",
                                channelId = NotificationHelper.CHANNEL_APPROVALS
                            )
                        } catch (ne: Exception) {
                            Log.w(TAG, "Notification trigger: ${ne.message}")
                        }
                    }
                } catch (refErr: Exception) {
                    Log.e(TAG, "Referral commission calculation error: ${refErr.message}", refErr)
                }
            }

            Log.d("MEDBANK_APPROVE", "Admin uid is still: " + FirebaseAuth.getInstance().currentUser?.uid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update access request in Firestore: ${e.message}")
        }

        dao.updateAccessRequest(
            AccessRequestEntity(
                id = req.id,
                uid = req.uid,
                studentName = req.studentName,
                cnic = req.cnic,
                scopeType = req.scopeType,
                scopeId = req.scopeId,
                planId = grantedPlanId,
                planName = grantedPlanName,
                paymentProofUrl = req.paymentProofUrl,
                whatsappSent = req.whatsappSent,
                status = newStatus,
                adminNote = adminNote,
                createdAt = req.createdAt,
                approvedAt = if (isApproved) System.currentTimeMillis() else null
            )
        )
    }

    // --- Referral Transactions Management ---
    fun observePendingReferralTransactions(): Flow<List<ReferralTransaction>> = callbackFlow {
        val ref = firestore.collection("referralTransactions")
            .whereEqualTo("status", "pending")
        val reg = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Pending transactions listener error: ${error.message}")
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { d ->
                ReferralTransaction(
                    id = d.id,
                    referrerUid = d.getString("referrerUid") ?: "",
                    referrerName = d.getString("referrerName") ?: "",
                    referredUid = d.getString("referredUid") ?: "",
                    referredName = d.getString("referredName") ?: "Student",
                    planName = d.getString("planName") ?: "Standard Pass",
                    amountPaid = d.getDouble("amountPaid") ?: (d.getLong("amountPaid")?.toDouble() ?: 0.0),
                    commissionAmount = d.getDouble("commissionAmount") ?: (d.getLong("commissionAmount")?.toDouble() ?: 0.0),
                    status = d.getString("status") ?: "pending",
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    paidAt = d.getTimestamp("paidAt")?.toDate()?.time
                )
            }?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun markReferralTransactionPaid(transactionId: String) {
        try {
            val update = mapOf(
                "status" to "paid",
                "paidAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("referralTransactions").document(transactionId).update(update).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark transaction paid: ${e.message}")
            throw e
        }
    }

    fun observeReferralTransactionsForUser(referrerUid: String): Flow<List<ReferralTransaction>> = callbackFlow {
        val ref = firestore.collection("referralTransactions")
            .whereEqualTo("referrerUid", referrerUid)
        val reg = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { d ->
                ReferralTransaction(
                    id = d.id,
                    referrerUid = d.getString("referrerUid") ?: "",
                    referrerName = d.getString("referrerName") ?: "",
                    referredUid = d.getString("referredUid") ?: "",
                    referredName = d.getString("referredName") ?: "Student",
                    planName = d.getString("planName") ?: "Standard Pass",
                    amountPaid = d.getDouble("amountPaid") ?: (d.getLong("amountPaid")?.toDouble() ?: 0.0),
                    commissionAmount = d.getDouble("commissionAmount") ?: (d.getLong("commissionAmount")?.toDouble() ?: 0.0),
                    status = d.getString("status") ?: "pending",
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    paidAt = d.getTimestamp("paidAt")?.toDate()?.time
                )
            }?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observePaidReferralTransactions(): Flow<List<ReferralTransaction>> = callbackFlow {
        val ref = firestore.collection("referralTransactions")
            .whereEqualTo("status", "paid")
        val reg = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { d ->
                ReferralTransaction(
                    id = d.id,
                    referrerUid = d.getString("referrerUid") ?: "",
                    referrerName = d.getString("referrerName") ?: "",
                    referredUid = d.getString("referredUid") ?: "",
                    referredName = d.getString("referredName") ?: "Student",
                    planName = d.getString("planName") ?: "Standard Pass",
                    amountPaid = d.getDouble("amountPaid") ?: (d.getLong("amountPaid")?.toDouble() ?: 0.0),
                    commissionAmount = d.getDouble("commissionAmount") ?: (d.getLong("commissionAmount")?.toDouble() ?: 0.0),
                    status = d.getString("status") ?: "paid",
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    paidAt = d.getTimestamp("paidAt")?.toDate()?.time
                )
            }?.sortedByDescending { it.paidAt ?: it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeApprovedAccessRequestsForUser(uid: String): Flow<List<AccessRequest>> = callbackFlow {
        val ref = firestore.collection("accessRequests")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "approved")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d ->
                AccessRequest(
                    id = d.id,
                    uid = d.getString("uid") ?: "",
                    studentName = d.getString("studentName") ?: "",
                    cnic = d.getString("cnic") ?: "",
                    scopeType = d.getString("scopeType") ?: "",
                    scopeId = d.getString("scopeId") ?: "",
                    planId = d.getString("planId") ?: "",
                    planName = d.getString("planName") ?: "",
                    status = d.getString("status") ?: "approved",
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    approvedAt = d.getTimestamp("approvedAt")?.toDate()?.time
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeNotifications(uid: String): Flow<List<NotificationItem>> = callbackFlow {
        val ref = firestore.collection("users").document(uid).collection("notifications")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d ->
                NotificationItem(
                    id = d.id,
                    title = d.getString("title") ?: "",
                    body = d.getString("body") ?: "",
                    type = d.getString("type") ?: "general",
                    isRead = d.getBoolean("isRead") ?: false,
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            }?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun markNotificationRead(uid: String, notificationId: String) {
        try {
            firestore.collection("users").document(uid).collection("notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed marking notification read: ${e.message}")
        }
    }

    suspend fun sendNotificationToUser(uid: String, title: String, body: String, type: String) {
        try {
            val notifId = UUID.randomUUID().toString()
            val data = mapOf(
                "title" to title,
                "body" to body,
                "type" to type,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(uid).collection("notifications").document(notifId).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending notification to user: ${e.message}")
        }
    }


    suspend fun getUserNameByUid(uid: String): String {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("fullName") ?: "Referrer"
        } catch (_: Exception) {
            "Referrer"
        }
    }


    // --- Membership Plans ---
    fun observeActivePlans(): Flow<List<MembershipPlan>> = callbackFlow {
        val ref = firestore.collection("membershipPlans").whereEqualTo("isActive", true).orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d -> firestoreDocToPlan(d) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeAllPlans(): Flow<List<MembershipPlan>> = callbackFlow {
        val ref = firestore.collection("membershipPlans").orderBy("order")
        val reg = ref.addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { d -> firestoreDocToPlan(d) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun getPlansForExamTrack(parentType: String, parentId: String): List<MembershipPlan> {
        return try {
            val snap = firestore.collection("membershipPlans")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            val allPlans = snap.documents.mapNotNull { firestoreDocToPlan(it) }
            val matching = allPlans.filter { plan ->
                val matchesProgram = plan.scopeProgramType.isBlank() || plan.scopeProgramType.equals(parentType, ignoreCase = true)
                val matchesYear = plan.scopeMbbsYear.isBlank() || plan.scopeMbbsYear.equals(parentId, ignoreCase = true) || parentId.contains(plan.scopeMbbsYear, ignoreCase = true)
                val matchesPathway = plan.scopeLicensingPathway.isBlank() || plan.scopeLicensingPathway.equals(parentId, ignoreCase = true) || parentId.contains(plan.scopeLicensingPathway, ignoreCase = true)
                val matchesScopeType = plan.scopeType.equals("all", ignoreCase = true) || 
                                       plan.scopeType.equals(parentType, ignoreCase = true) || 
                                       plan.scopeIds.contains(parentId)
                (matchesProgram && matchesYear && matchesPathway) || matchesScopeType
            }
            if (matching.isNotEmpty()) {
                matching
            } else if (allPlans.isNotEmpty()) {
                allPlans
            } else {
                listOf(
                    MembershipPlan(
                        id = "plan_${parentType}_30d",
                        name = "${parentType.uppercase()} 30-Day Pass",
                        price = 29.99,
                        currency = "USD",
                        durationDays = 30,
                        scopeType = parentType,
                        scopeProgramType = parentType,
                        features = listOf("Full high-yield question explanations", "Timed mock mode with negative marking", "Unlimited access")
                    ),
                    MembershipPlan(
                        id = "plan_${parentType}_90d",
                        name = "${parentType.uppercase()} 90-Day Full Access",
                        price = 59.99,
                        currency = "USD",
                        durationDays = 90,
                        scopeType = parentType,
                        scopeProgramType = parentType,
                        features = listOf("Full high-yield question explanations", "Timed mock mode with negative marking", "Unlimited retakes", "Priority support")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching plans: ${e.message}")
            listOf(
                MembershipPlan(
                    id = "plan_standard_30d",
                    name = "Standard 30-Day Clinical Pass",
                    price = 29.99,
                    currency = "USD",
                    durationDays = 30,
                    scopeType = "all",
                    features = listOf("Full explanations", "Timed mock mode", "Negative marking")
                )
            )
        }
    }

    private fun firestoreDocToPlan(d: com.google.firebase.firestore.DocumentSnapshot): MembershipPlan? {
        if (!d.exists()) return null
        return MembershipPlan(
            id = d.id,
            name = d.getString("name") ?: "",
            price = d.getDouble("price") ?: (d.getLong("price")?.toDouble() ?: 0.0),
            currency = d.getString("currency") ?: "USD",
            durationDays = d.getLong("durationDays")?.toInt() ?: 30,
            scopeType = d.getString("scopeType") ?: d.getString("scopeProgramType") ?: "all",
            scopeProgramType = d.getString("scopeProgramType") ?: "",
            scopeMbbsYear = d.getString("scopeMbbsYear") ?: "",
            scopeLicensingPathway = d.getString("scopeLicensingPathway") ?: "",
            scopeIds = (d.get("scopeIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            features = (d.get("features") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
            isActive = d.getBoolean("isActive") ?: true,
            order = d.getLong("order")?.toInt() ?: 0
        )
    }

    suspend fun savePlan(plan: MembershipPlan) {
        val data = hashMapOf(
            "name" to plan.name,
            "price" to plan.price,
            "currency" to plan.currency,
            "durationDays" to plan.durationDays,
            "scopeType" to plan.scopeType,
            "scopeIds" to plan.scopeIds,
            "features" to plan.features,
            "isActive" to plan.isActive,
            "order" to plan.order
        )
        firestore.collection("membershipPlans").document(plan.id).set(data).await()
        dao.insertPlan(
            MembershipPlanEntity(
                id = plan.id,
                name = plan.name,
                price = plan.price,
                currency = plan.currency,
                durationDays = plan.durationDays,
                scopeType = plan.scopeType,
                scopeIdsJson = listToJson(plan.scopeIds),
                featuresJson = listToJson(plan.features),
                isActive = plan.isActive,
                orderIndex = plan.order
            )
        )
    }

    suspend fun deletePlan(planId: String) {
        try {
            firestore.collection("membershipPlans").document(planId).delete().await()
        } catch (_: Exception) {}
        val plan = dao.getPlanById(planId) ?: return
        dao.deletePlan(plan)
    }

    // --- STEP 8: Bulk Import Parser ---
    fun parseBulkImport(rawContent: String): BulkImportResult {
        val lines = rawContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        val errors = mutableListOf<String>()

        if (lines.isEmpty()) {
            return BulkImportResult(false, null, emptyList(), emptyList(), listOf("Input text is empty."))
        }

        fun splitPipes(line: String): List<String> {
            val tokens = mutableListOf<String>()
            val sb = StringBuilder()
            var inQuotes = false
            for (ch in line) {
                if (ch == '\"') {
                    inQuotes = !inQuotes
                } else if (ch == '|' && !inQuotes) {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                } else {
                    sb.append(ch)
                }
            }
            tokens.add(sb.toString().trim())
            return tokens
        }

        val firstLine = lines.first()
        val firstTokens = splitPipes(firstLine)

        if (firstTokens.isEmpty() || firstTokens[0] != "EXAM") {
            return BulkImportResult(false, null, emptyList(), emptyList(), listOf("Line 1 must start with EXAM|..."))
        }

        if (firstTokens.size < 10) {
            errors.add("EXAM metadata must have 10 columns: EXAM|title|subject|parentType|parentId|durationMin|totalMarks|negativeMarking|planId|status.")
            return BulkImportResult(false, null, emptyList(), emptyList(), errors)
        }

        val examTitle = firstTokens[1]
        val subjectName = firstTokens[2]
        val parentType = firstTokens[3].lowercase()
        val parentId = firstTokens[4]
        val durationMin = firstTokens[5].toIntOrNull() ?: 60
        val totalMarks = firstTokens[6].toDoubleOrNull() ?: 100.0
        val negativeMarking = firstTokens[7].toDoubleOrNull() ?: 0.25
        val planId = firstTokens[8]
        val status = firstTokens[9].lowercase()
        val isPublished = status == "published" || status == "active"

        val examId = "exam_${UUID.randomUUID().toString().take(8)}"
        val subjectId = "subj_${subjectName.lowercase().replace(" ", "_")}"

        val questions = mutableListOf<Question>()
        val answers = mutableListOf<PrivateAnswer>()

        val qLines = lines.drop(1)
        if (qLines.size > 200) {
            errors.add("Exceeded maximum of 200 questions per exam (found ${qLines.size}).")
        }

        qLines.forEachIndexed { idx, line ->
            val lineNum = idx + 2
            val tokens = splitPipes(line)
            if (tokens.isEmpty() || tokens[0] != "Q") {
                errors.add("Line $lineNum: Must start with Q|...")
                return@forEachIndexed
            }

            if (tokens.size < 8) {
                errors.add("Line $lineNum: Question has insufficient columns (expected at least 8).")
                return@forEachIndexed
            }

            val qText = tokens[1]
            val optA = tokens[2]
            val optB = tokens[3]
            val optC = tokens[4]
            val optD = tokens[5]
            val correctKey = tokens[6].trim().uppercase()
            val explanation = tokens.getOrNull(7) ?: ""
            val difficulty = tokens.getOrNull(8)?.ifBlank { "medium" } ?: "medium"
            val tags = tokens.getOrNull(9)?.split(";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            if (qText.isBlank()) errors.add("Line $lineNum: Question text cannot be empty.")
            if (optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
                errors.add("Line $lineNum: Options A, B, C, and D must not be empty.")
            }

            val correctIndex = when (correctKey) {
                "A", "0" -> 0
                "B", "1" -> 1
                "C", "2" -> 2
                "D", "3" -> 3
                else -> {
                    errors.add("Line $lineNum: Correct answer must be A, B, C, or D. Found '$correctKey'.")
                    -1
                }
            }

            val qid = "q_${idx + 1}"
            questions.add(
                Question(
                    id = qid,
                    examId = examId,
                    order = idx + 1,
                    text = qText,
                    options = listOf(optA, optB, optC, optD),
                    difficulty = difficulty,
                    tags = tags
                )
            )
            answers.add(
                PrivateAnswer(
                    id = "${examId}_${qid}",
                    examId = examId,
                    qid = qid,
                    correctIndex = correctIndex.coerceAtLeast(0),
                    explanation = explanation
                )
            )
        }

        if (questions.isEmpty()) {
            errors.add("No valid questions found to import.")
        }

        val parsedExam = Exam(
            id = examId,
            title = examTitle,
            subjectId = subjectId,
            parentType = parentType,
            parentId = parentId,
            durationMin = durationMin,
            totalMarks = totalMarks,
            negativeMarking = negativeMarking,
            maxAttempts = 1,
            offlineEnabled = true,
            isPublished = isPublished,
            accessPlanIds = if (planId.isNotBlank()) listOf(planId) else emptyList(),
            questionCount = questions.size
        )

        return BulkImportResult(
            success = errors.isEmpty(),
            exam = parsedExam,
            questions = questions,
            answers = answers,
            errors = errors
        )
    }

    suspend fun commitBulkImport(
        exam: Exam,
        subjectName: String,
        questions: List<Question>,
        answers: List<PrivateAnswer>
    ) {
        // Ensure Subject exists in Firestore
        val subjDoc = firestore.collection("subjects").document(exam.subjectId).get().await()
        if (!subjDoc.exists()) {
            firestore.collection("subjects").document(exam.subjectId).set(
                mapOf(
                    "parentType" to exam.parentType,
                    "parentId" to exam.parentId,
                    "name" to subjectName,
                    "order" to 1,
                    "isActive" to true
                )
            ).await()
        }

        // Save Exam to Firestore
        saveExam(exam)

        // Save Questions to exams/{examId}/questions in batch
        val batch = firestore.batch()
        questions.forEach { q ->
            val qRef = firestore.collection("exams").document(exam.id).collection("questions").document(q.id)
            batch.set(
                qRef,
                mapOf(
                    "order" to q.order,
                    "text" to q.text,
                    "options" to q.options,
                    "difficulty" to q.difficulty,
                    "tags" to q.tags
                )
            )
        }

        // Save Private Answers to examPrivate/{examId}/answers in batch
        answers.forEach { a ->
            val aRef = firestore.collection("examPrivate").document(exam.id).collection("answers").document(a.qid)
            batch.set(
                aRef,
                mapOf(
                    "correctIndex" to a.correctIndex,
                    "explanation" to a.explanation
                )
            )
        }

        batch.commit().await()

        // Also cache locally in Room
        dao.insertSubject(SubjectEntity(exam.subjectId, exam.parentType, exam.parentId, subjectName, 1, true))
        val qEntities = questions.map {
            QuestionEntity(it.id, exam.id, it.order, it.text, listToJson(it.options), it.difficulty, listToJson(it.tags))
        }
        dao.insertQuestions(qEntities)
        val aEntities = answers.map {
            PrivateAnswerEntity("${exam.id}_${it.qid}", exam.id, it.qid, it.correctIndex, it.explanation)
        }
        dao.insertAnswers(aEntities)
    }

    // --- Dashboard Stats for Admin ---
    suspend fun getAdminDashboardStats(): Map<String, Int> {
        val userCount = try {
            firestore.collection("users").get().await().size()
        } catch (_: Exception) { dao.getUserCount() }

        val pendingRequests = try {
            firestore.collection("accessRequests").whereEqualTo("status", "pending").get().await().size()
        } catch (_: Exception) { dao.getPendingRequestCount() }

        val publishedExams = try {
            firestore.collection("exams").whereEqualTo("isPublished", true).get().await().size()
        } catch (_: Exception) { dao.getPublishedExamCount() }

        val activePlans = try {
            firestore.collection("membershipPlans").whereEqualTo("isActive", true).get().await().size()
        } catch (_: Exception) { dao.getActivePlanCount() }

        val activeSubscriptions = try {
            val approvedSnap = firestore.collection("accessRequests").whereEqualTo("status", "approved").get().await()
            approvedSnap.size()
        } catch (_: Exception) {
            dao.getActivePlanCount()
        }

        val pendingPayouts = try {
            firestore.collection("referralTransactions").whereEqualTo("status", "pending").get().await().size()
        } catch (_: Exception) { 0 }

        return mapOf(
            "users" to userCount,
            "totalStudents" to userCount,
            "pendingRequests" to pendingRequests,
            "publishedExams" to publishedExams,
            "activePlans" to activePlans,
            "activeSubscriptions" to activeSubscriptions,
            "pendingPayouts" to pendingPayouts
        )
    }

    suspend fun broadcastNotification(title: String, message: String) {
        try {
            val data = mapOf(
                "title" to title,
                "message" to message,
                "topic" to "medbank_announcements",
                "createdAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("broadcasts").add(data).await()
            val settings = getSettings()
            updateSettings(settings.adminWhatsapp, "$title: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast notification: ${e.message}")
        }
    }

    suspend fun getSubjectsByParent(parentType: String, parentId: String): List<Subject> {
        return try {
            val snap = firestore.collection("subjects")
                .whereEqualTo("parentType", parentType)
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("isActive", true)
                .get().await()
            snap.documents.map { d ->
                Subject(d.id, d.getString("parentType") ?: "", d.getString("parentId") ?: "", d.getString("name") ?: "", d.getLong("order")?.toInt() ?: 0, d.getBoolean("isActive") ?: true)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getExamsBySubject(subjectId: String): List<Exam> {
        return try {
            val snap = firestore.collection("exams")
                .whereEqualTo("subjectId", subjectId)
                .get().await()
            snap.documents.mapNotNull { firestoreDocToExam(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAllPlans(): List<MembershipPlan> {
        return try {
            val snap = firestore.collection("membershipPlans").orderBy("order").get().await()
            snap.documents.mapNotNull { firestoreDocToPlan(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseContextBulkImport(rawContent: String, examId: String): BulkImportResult {
        val lines = rawContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        val errors = mutableListOf<String>()

        if (lines.isEmpty()) {
            return BulkImportResult(false, null, emptyList(), emptyList(), listOf("Input text is empty."))
        }

        fun splitPipes(line: String): List<String> {
            val tokens = mutableListOf<String>()
            val sb = StringBuilder()
            var inQuotes = false
            for (ch in line) {
                if (ch == '\"') {
                    inQuotes = !inQuotes
                } else if (ch == '|' && !inQuotes) {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                } else {
                    sb.append(ch)
                }
            }
            tokens.add(sb.toString().trim())
            return tokens
        }

        if (lines.size > 200) {
            errors.add("Exceeded maximum of 200 questions per exam (found ${lines.size}).")
        }

        val questions = mutableListOf<Question>()
        val answers = mutableListOf<PrivateAnswer>()

        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1
            val tokens = splitPipes(line)
            if (tokens.isEmpty() || tokens[0] != "Q") {
                errors.add("Line $lineNum: Must start with Q|...")
                return@forEachIndexed
            }

            if (tokens.size < 8) {
                errors.add("Line $lineNum: Question has insufficient columns (expected at least 8).")
                return@forEachIndexed
            }

            val qText = tokens[1]
            val optA = tokens[2]
            val optB = tokens[3]
            val optC = tokens[4]
            val optD = tokens[5]
            val correctKey = tokens[6].trim().uppercase()
            val explanation = tokens.getOrNull(7) ?: ""
            val difficulty = tokens.getOrNull(8)?.ifBlank { "medium" } ?: "medium"
            val tags = tokens.getOrNull(9)?.split(";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

            if (qText.isBlank()) errors.add("Line $lineNum: Question text cannot be empty.")
            if (optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
                errors.add("Line $lineNum: Options A, B, C, and D must not be empty.")
            }

            val correctIndex = when (correctKey) {
                "A", "0" -> 0
                "B", "1" -> 1
                "C", "2" -> 2
                "D", "3" -> 3
                else -> {
                    errors.add("Line $lineNum: Correct answer must be A, B, C, or D. Found '$correctKey'.")
                    -1
                }
            }

            val qid = "q_${System.currentTimeMillis()}_${idx + 1}"
            questions.add(
                Question(
                    id = qid,
                    examId = examId,
                    order = idx + 1,
                    text = qText,
                    options = listOf(optA, optB, optC, optD).filter { it.isNotBlank() },
                    difficulty = difficulty,
                    tags = tags
                )
            )
            answers.add(
                PrivateAnswer(
                    id = "${examId}_${qid}",
                    examId = examId,
                    qid = qid,
                    correctIndex = correctIndex.coerceAtLeast(0),
                    explanation = explanation
                )
            )
        }

        if (questions.isEmpty()) {
            errors.add("No valid questions found to import.")
        }

        return BulkImportResult(
            success = errors.isEmpty(),
            exam = null,
            questions = questions,
            answers = answers,
            errors = errors
        )
    }

    suspend fun commitContextBulkImport(
        examId: String,
        questions: List<Question>,
        answers: List<PrivateAnswer>,
        replaceMode: Boolean
    ) {
        val examDocRef = firestore.collection("exams").document(examId)
        val examSnap = examDocRef.get().await()
        val existingExam = if (examSnap.exists()) firestoreDocToExam(examSnap) else null
        val existingCount = existingExam?.questionCount ?: 0

        if (replaceMode) {
            try {
                val qDocs = examDocRef.collection("questions").get().await()
                val aDocs = firestore.collection("examPrivate").document(examId).collection("answers").get().await()
                val batchDel = firestore.batch()
                qDocs.documents.forEach { batchDel.delete(it.reference) }
                aDocs.documents.forEach { batchDel.delete(it.reference) }
                batchDel.commit().await()
            } catch (_: Exception) {}
        }

        val batch = firestore.batch()
        val startOrder = if (replaceMode) 1 else (existingCount + 1)

        questions.forEachIndexed { idx, q ->
            val finalOrder = startOrder + idx
            val qRef = examDocRef.collection("questions").document(q.id)
            batch.set(
                qRef,
                mapOf(
                    "order" to finalOrder,
                    "text" to q.text,
                    "options" to q.options,
                    "difficulty" to q.difficulty,
                    "tags" to q.tags
                )
            )
        }

        answers.forEach { a ->
            val aRef = firestore.collection("examPrivate").document(examId).collection("answers").document(a.qid)
            batch.set(
                aRef,
                mapOf(
                    "correctIndex" to a.correctIndex,
                    "explanation" to a.explanation
                )
            )
        }

        val newTotalCount = if (replaceMode) questions.size else (existingCount + questions.size)
        batch.update(examDocRef, "questionCount", newTotalCount, "updatedAt", FieldValue.serverTimestamp())
        batch.commit().await()

        existingExam?.let {
            val updatedExam = it.copy(questionCount = newTotalCount)
            dao.insertExam(ExamEntity(
                updatedExam.id, updatedExam.title, updatedExam.subjectId, updatedExam.parentType, updatedExam.parentId,
                updatedExam.durationMin, updatedExam.totalMarks, updatedExam.negativeMarking, updatedExam.maxAttempts,
                updatedExam.offlineEnabled, updatedExam.isPublished, listToJson(updatedExam.accessPlanIds), updatedExam.questionCount,
                updatedExam.createdAt, updatedExam.updatedAt
            ))
        }
    }
}
