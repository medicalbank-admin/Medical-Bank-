package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedBankDao {

    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 'app' LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 'app' LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    // --- Users & Auth ---
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUserByUid(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE cnic = :cnic LIMIT 1")
    suspend fun getUserByCnic(cnic: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- CNIC Index ---
    @Query("SELECT * FROM cnic_index WHERE cnic = :cnic LIMIT 1")
    suspend fun getCnicIndex(cnic: String): CnicIndexEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCnicIndex(index: CnicIndexEntity)

    // --- Devices ---
    @Query("SELECT * FROM user_devices WHERE uid = :uid AND status = 'active' LIMIT 1")
    suspend fun getActiveDeviceForUser(uid: String): UserDeviceEntity?

    @Query("SELECT * FROM user_devices WHERE uid = :uid")
    fun observeDevicesForUser(uid: String): Flow<List<UserDeviceEntity>>

    @Query("SELECT * FROM user_devices ORDER BY lastSeen DESC")
    fun observeAllDevices(): Flow<List<UserDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: UserDeviceEntity)

    @Query("UPDATE user_devices SET status = 'revoked' WHERE uid = :uid")
    suspend fun revokeAllDevicesForUser(uid: String)

    // --- Programs ---
    @Query("SELECT * FROM programs WHERE isActive = 1 ORDER BY orderIndex ASC")
    fun observeActivePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs ORDER BY orderIndex ASC")
    fun observeAllPrograms(): Flow<List<ProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)

    @Delete
    suspend fun deleteProgram(program: ProgramEntity)

    // --- MBBS Years ---
    @Query("SELECT * FROM mbbs_years WHERE isActive = 1 ORDER BY yearNumber ASC")
    fun observeActiveYears(): Flow<List<MbbsYearEntity>>

    @Query("SELECT * FROM mbbs_years ORDER BY yearNumber ASC")
    fun observeAllYears(): Flow<List<MbbsYearEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYear(year: MbbsYearEntity)

    @Delete
    suspend fun deleteYear(year: MbbsYearEntity)

    // --- Licensing Pathways ---
    @Query("SELECT * FROM licensing_pathways WHERE isActive = 1 ORDER BY orderIndex ASC")
    fun observeActivePathways(): Flow<List<LicensingPathwayEntity>>

    @Query("SELECT * FROM licensing_pathways ORDER BY orderIndex ASC")
    fun observeAllPathways(): Flow<List<LicensingPathwayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPathway(pathway: LicensingPathwayEntity)

    @Delete
    suspend fun deletePathway(pathway: LicensingPathwayEntity)

    // --- Subjects ---
    @Query("SELECT * FROM subjects WHERE parentType = :parentType AND parentId = :parentId AND isActive = 1 ORDER BY orderIndex ASC")
    fun observeSubjectsByParent(parentType: String, parentId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE parentType = :parentType AND isActive = 1 ORDER BY orderIndex ASC")
    fun observeSubjectsByParentType(parentType: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE parentType = :parentType AND parentId = :parentId ORDER BY orderIndex ASC")
    fun observeAllSubjectsByParent(parentType: String, parentId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    // --- Exams ---
    @Query("SELECT * FROM exams WHERE subjectId = :subjectId AND isPublished = 1 ORDER BY createdAt DESC")
    fun observePublishedExamsBySubject(subjectId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE parentType = :parentType AND isPublished = 1 ORDER BY createdAt DESC")
    fun observePublishedExamsByParentType(parentType: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE parentType = :parentType AND parentId = :parentId AND isPublished = 1 ORDER BY createdAt DESC")
    fun observePublishedExamsByParent(parentType: String, parentId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun observeAllExamsBySubject(subjectId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun observeAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): ExamEntity?

    @Query("SELECT COUNT(*) FROM exams WHERE isPublished = 1")
    suspend fun getPublishedExamCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    // --- Questions ---
    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY orderIndex ASC")
    fun observeQuestionsByExam(examId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY orderIndex ASC")
    suspend fun getQuestionsByExam(examId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE examId = :examId")
    suspend fun deleteQuestionsByExam(examId: String)

    // --- Private Answers (Server-Side Answers) ---
    @Query("SELECT * FROM private_answers WHERE examId = :examId")
    suspend fun getAnswersByExam(examId: String): List<PrivateAnswerEntity>

    @Query("SELECT * FROM private_answers WHERE examId = :examId AND qid = :qid LIMIT 1")
    suspend fun getAnswerForQuestion(examId: String, qid: String): PrivateAnswerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<PrivateAnswerEntity>)

    @Query("DELETE FROM private_answers WHERE examId = :examId")
    suspend fun deleteAnswersByExam(examId: String)

    // --- Access Requests ---
    @Query("SELECT * FROM access_requests ORDER BY createdAt DESC")
    fun observeAllAccessRequests(): Flow<List<AccessRequestEntity>>

    @Query("SELECT * FROM access_requests WHERE uid = :uid ORDER BY createdAt DESC")
    fun observeAccessRequestsByUser(uid: String): Flow<List<AccessRequestEntity>>

    @Query("SELECT * FROM access_requests WHERE uid = :uid AND scopeId = :scopeId AND status = 'pending' LIMIT 1")
    suspend fun getPendingRequest(uid: String, scopeId: String): AccessRequestEntity?

    @Query("SELECT COUNT(*) FROM access_requests WHERE status = 'pending'")
    suspend fun getPendingRequestCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessRequest(request: AccessRequestEntity)

    @Update
    suspend fun updateAccessRequest(request: AccessRequestEntity)

    // --- Membership Plans ---
    @Query("SELECT * FROM membership_plans WHERE isActive = 1 ORDER BY orderIndex ASC")
    fun observeActivePlans(): Flow<List<MembershipPlanEntity>>

    @Query("SELECT * FROM membership_plans ORDER BY orderIndex ASC")
    fun observeAllPlans(): Flow<List<MembershipPlanEntity>>

    @Query("SELECT * FROM membership_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): MembershipPlanEntity?

    @Query("SELECT COUNT(*) FROM membership_plans WHERE isActive = 1")
    suspend fun getActivePlanCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MembershipPlanEntity)

    @Update
    suspend fun updatePlan(plan: MembershipPlanEntity)

    @Delete
    suspend fun deletePlan(plan: MembershipPlanEntity)

    // --- Exam Results ---
    @Query("SELECT * FROM exam_results WHERE uid = :uid ORDER BY submittedAt DESC")
    fun observeResultsByUser(uid: String): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE uid = :uid AND examId = :examId ORDER BY attemptNumber DESC")
    suspend fun getResultsForExamByUser(uid: String, examId: String): List<ExamResultEntity>

    @Query("SELECT * FROM exam_results WHERE id = :id LIMIT 1")
    suspend fun getResultById(id: String): ExamResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ExamResultEntity)
}
