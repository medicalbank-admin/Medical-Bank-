package com.example.data.models

data class User(
    val uid: String = "",
    val role: String = "student", // "student" or "admin"
    val fullName: String = "",
    val cnic: String = "", // 13 digits
    val email: String = "",
    val phone: String = "",
    val country: String = "",
    val photoUrl: String = "",
    // TODO: Requires Cloud Storage — use Base64 in Firestore for now.
    val photoBase64: String = "",
    val programType: String = "mbbs", // "mbbs" or "licensing"
    val mbbsYear: Int? = null, // 1 to 6
    val licensingPath: String? = null, // "NRE", "FCPS", "USMLE", "PLAB", "DHA", "SMLE"
    val entitlements: Map<String, Entitlement> = emptyMap(), // scopeId -> Entitlement
    val fcmToken: String = "",
    val fcmTokenUpdatedAt: Long = 0L,
    val subscribedTopics: List<String> = emptyList(),
    val notificationOptIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val referralCode: String = "",
    val referredBy: String? = null,
    val walletBalance: Double = 0.0,
    val totalEarned: Double = 0.0
)

data class Entitlement(
    val planId: String = "",
    val planName: String = "",
    val expiresAt: Long = 0L,
    val status: String = "active" // "active" or "expired"
)

data class Program(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "mbbs" or "licensing"
    val order: Int = 0,
    val isActive: Boolean = true
)

data class MbbsYear(
    val id: String = "",
    val programId: String = "mbbs",
    val name: String = "", // e.g. "Year 1"
    val yearNumber: Int = 1,
    val order: Int = 0,
    val isActive: Boolean = true
)

data class LicensingPathway(
    val id: String = "",
    val programId: String = "licensing",
    val name: String = "", // e.g. "USMLE Step 1"
    val code: String = "", // e.g. "USMLE"
    val order: Int = 0,
    val isActive: Boolean = true
)

data class Subject(
    val id: String = "",
    val parentType: String = "", // "mbbs" or "licensing"
    val parentId: String = "", // e.g. yearId "year_1" or pathwayId "usmle"
    val name: String = "",
    val order: Int = 0,
    val isActive: Boolean = true
)

data class Exam(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val parentType: String = "", // "mbbs" or "licensing"
    val parentId: String = "",
    val durationMin: Int = 60,
    val totalMarks: Double = 100.0,
    val negativeMarking: Double = 0.25, // deduction for wrong answer
    val maxAttempts: Int = 1, // 1, 2, or 3
    val offlineEnabled: Boolean = false,
    val isPublished: Boolean = true,
    val accessPlanIds: List<String> = emptyList(),
    val questionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Question(
    val id: String = "",
    val examId: String = "",
    val order: Int = 0,
    val text: String = "",
    val options: List<String> = emptyList(), // exactly 4 options
    val difficulty: String = "medium", // "easy", "medium", "hard"
    val tags: List<String> = emptyList()
)

data class PrivateAnswer(
    val id: String = "",
    val examId: String = "",
    val qid: String = "",
    val correctIndex: Int = 0, // 0 for A, 1 for B, 2 for C, 3 for D
    val explanation: String = ""
)

data class AccessRequest(
    val id: String = "",
    val uid: String = "",
    val studentName: String = "",
    val cnic: String = "",
    val scopeType: String = "", // "program", "year", "pathway", "subject", "exam"
    val scopeId: String = "",
    val planId: String = "",
    val planName: String = "",
    val paymentProofUrl: String = "",
    // TODO: Requires Cloud Storage — use Base64 in Firestore for now.
    val paymentProofBase64: String = "",
    val whatsappSent: Boolean = false,
    val status: String = "pending", // "pending", "approved", "rejected"
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null
)

data class MembershipPlan(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val currency: String = "USD",
    val durationDays: Int = 30,
    val scopeType: String = "all", // "all", "mbbs", "licensing", "pathway", "year", "exam"
    val scopeProgramType: String = "",
    val scopeMbbsYear: String = "",
    val scopeLicensingPathway: String = "",
    val scopeIds: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val isActive: Boolean = true,
    val order: Int = 0
)

data class StudentAnswer(
    val qid: String = "",
    val selectedIndex: Int = -1 // -1 means skipped
)

data class ExamResult(
    val id: String = "",
    val uid: String = "",
    val examId: String = "",
    val examTitle: String = "",
    val score: Double = 0.0,
    val totalMarks: Double = 0.0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val skippedCount: Int = 0,
    val answers: List<StudentAnswer> = emptyList(),
    val startedAt: Long = 0L,
    val submittedAt: Long = 0L,
    val attemptNumber: Int = 1,
    val syncStatus: String = "synced", // "pending" or "synced"
    val deviceId: String = ""
)

data class UserDevice(
    val deviceId: String = "",
    val uid: String = "",
    val platform: String = "Android",
    val model: String = "",
    val appVersion: String = "1.0",
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val status: String = "active" // "active" or "revoked"
)

data class AppSettings(
    val firstAdminClaimed: Boolean = false,
    val maxDevicesPerUser: Int = 1,
    val offlinePackExpiryDays: Int = 7,
    val adminWhatsapp: String = "+1234567890",
    val announcementBanner: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class BulkImportResult(
    val success: Boolean,
    val exam: Exam?,
    val questions: List<Question>,
    val answers: List<PrivateAnswer>,
    val errors: List<String>
)

data class LegalContent(
    val privacyPolicy: String = "",
    val termsAndConditions: String = "",
    val updatedAt: Long = 0L
)

data class ReferralTransaction(
    val id: String = "",
    val referrerUid: String = "",
    val referrerName: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val planName: String = "",
    val amountPaid: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val status: String = "pending", // "pending", "paid"
    val createdAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null
)

data class RegistrationResult(
    val user: User,
    val invalidReferralCodeEntered: Boolean = false
)

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "general", // "referral", "exam", "general"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)



