package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val role: String, // "student" or "admin"
    val fullName: String,
    val cnic: String, // 13 digits
    val email: String,
    val passwordHash: String,
    val phone: String,
    val country: String,
    val photoUrl: String,
    val programType: String, // "mbbs" or "licensing"
    val mbbsYear: Int?,
    val licensingPath: String?,
    val entitlementsJson: String, // JSON map of scopeId -> Entitlement
    val createdAt: Long,
    val lastLoginAt: Long,
    val referralCode: String = "",
    val referredBy: String? = null,
    val walletBalance: Double = 0.0,
    val totalEarned: Double = 0.0
)

@Entity(tableName = "cnic_index")
data class CnicIndexEntity(
    @PrimaryKey val cnic: String,
    val uid: String,
    val email: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: String = "app",
    val firstAdminClaimed: Boolean,
    val maxDevicesPerUser: Int,
    val offlinePackExpiryDays: Int,
    val adminWhatsapp: String,
    val announcementBanner: String,
    val updatedAt: Long
)

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val orderIndex: Int,
    val isActive: Boolean
)

@Entity(tableName = "mbbs_years")
data class MbbsYearEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val name: String,
    val yearNumber: Int,
    val orderIndex: Int,
    val isActive: Boolean
)

@Entity(tableName = "licensing_pathways")
data class LicensingPathwayEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val name: String,
    val code: String,
    val orderIndex: Int,
    val isActive: Boolean
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val parentType: String,
    val parentId: String,
    val name: String,
    val orderIndex: Int,
    val isActive: Boolean
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subjectId: String,
    val parentType: String,
    val parentId: String,
    val durationMin: Int,
    val totalMarks: Double,
    val negativeMarking: Double,
    val maxAttempts: Int,
    val offlineEnabled: Boolean,
    val isPublished: Boolean,
    val accessPlanIdsJson: String,
    val questionCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val orderIndex: Int,
    val text: String,
    val optionsJson: String, // JSON array of 4 options
    val difficulty: String,
    val tagsJson: String
)

@Entity(tableName = "private_answers")
data class PrivateAnswerEntity(
    @PrimaryKey val id: String, // "${examId}_${qid}"
    val examId: String,
    val qid: String,
    val correctIndex: Int,
    val explanation: String
)

@Entity(tableName = "access_requests")
data class AccessRequestEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val studentName: String,
    val cnic: String,
    val scopeType: String,
    val scopeId: String,
    val planId: String,
    val planName: String,
    val paymentProofUrl: String,
    val whatsappSent: Boolean,
    val status: String, // "pending", "approved", "rejected"
    val adminNote: String,
    val createdAt: Long,
    val approvedAt: Long?
)

@Entity(tableName = "membership_plans")
data class MembershipPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val currency: String,
    val durationDays: Int,
    val scopeType: String,
    val scopeIdsJson: String,
    val featuresJson: String,
    val isActive: Boolean,
    val orderIndex: Int
)

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val examId: String,
    val examTitle: String,
    val score: Double,
    val totalMarks: Double,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val answersJson: String, // JSON array of answers
    val startedAt: Long,
    val submittedAt: Long,
    val attemptNumber: Int,
    val syncStatus: String,
    val deviceId: String
)

@Entity(tableName = "user_devices")
data class UserDeviceEntity(
    @PrimaryKey val deviceId: String,
    val uid: String,
    val platform: String,
    val model: String,
    val appVersion: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val status: String
)
