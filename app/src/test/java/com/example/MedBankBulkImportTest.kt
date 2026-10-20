package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.MedBankRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeDao : MedBankDao {
    override fun observeSettings(): Flow<SettingsEntity?> = emptyFlow()
    override suspend fun getSettings(): SettingsEntity? = null
    override suspend fun insertSettings(settings: SettingsEntity) {}
    override suspend fun getUserByUid(uid: String): UserEntity? = null
    override fun observeUserByUid(uid: String): Flow<UserEntity?> = emptyFlow()
    override suspend fun getUserByCnic(cnic: String): UserEntity? = null
    override fun observeAllUsers(): Flow<List<UserEntity>> = emptyFlow()
    override suspend fun getUserCount(): Int = 0
    override suspend fun insertUser(user: UserEntity) {}
    override suspend fun updateUser(user: UserEntity) {}
    override suspend fun getCnicIndex(cnic: String): CnicIndexEntity? = null
    override suspend fun insertCnicIndex(index: CnicIndexEntity) {}
    override suspend fun getActiveDeviceForUser(uid: String): UserDeviceEntity? = null
    override fun observeDevicesForUser(uid: String): Flow<List<UserDeviceEntity>> = emptyFlow()
    override fun observeAllDevices(): Flow<List<UserDeviceEntity>> = emptyFlow()
    override suspend fun insertDevice(device: UserDeviceEntity) {}
    override suspend fun revokeAllDevicesForUser(uid: String) {}
    override fun observeActivePrograms(): Flow<List<ProgramEntity>> = emptyFlow()
    override fun observeAllPrograms(): Flow<List<ProgramEntity>> = emptyFlow()
    override suspend fun insertProgram(program: ProgramEntity) {}
    override suspend fun deleteProgram(program: ProgramEntity) {}
    override fun observeActiveYears(): Flow<List<MbbsYearEntity>> = emptyFlow()
    override fun observeAllYears(): Flow<List<MbbsYearEntity>> = emptyFlow()
    override suspend fun insertYear(year: MbbsYearEntity) {}
    override suspend fun deleteYear(year: MbbsYearEntity) {}
    override fun observeActivePathways(): Flow<List<LicensingPathwayEntity>> = emptyFlow()
    override fun observeAllPathways(): Flow<List<LicensingPathwayEntity>> = emptyFlow()
    override suspend fun insertPathway(pathway: LicensingPathwayEntity) {}
    override suspend fun deletePathway(pathway: LicensingPathwayEntity) {}
    override fun observeSubjectsByParent(parentType: String, parentId: String): Flow<List<SubjectEntity>> = emptyFlow()
    override fun observeSubjectsByParentType(parentType: String): Flow<List<SubjectEntity>> = emptyFlow()
    override fun observeAllSubjectsByParent(parentType: String, parentId: String): Flow<List<SubjectEntity>> = emptyFlow()
    override suspend fun getSubjectById(id: String): SubjectEntity? = null
    override fun observeAllSubjects(): Flow<List<SubjectEntity>> = emptyFlow()
    override suspend fun insertSubject(subject: SubjectEntity) {}
    override suspend fun deleteSubject(subject: SubjectEntity) {}
    override fun observePublishedExamsBySubject(subjectId: String): Flow<List<ExamEntity>> = emptyFlow()
    override fun observePublishedExamsByParentType(parentType: String): Flow<List<ExamEntity>> = emptyFlow()
    override fun observePublishedExamsByParent(parentType: String, parentId: String): Flow<List<ExamEntity>> = emptyFlow()
    override fun observeAllExamsBySubject(subjectId: String): Flow<List<ExamEntity>> = emptyFlow()
    override fun observeAllExams(): Flow<List<ExamEntity>> = emptyFlow()
    override suspend fun getExamById(id: String): ExamEntity? = null
    override suspend fun getPublishedExamCount(): Int = 0
    override suspend fun insertExam(exam: ExamEntity) {}
    override suspend fun updateExam(exam: ExamEntity) {}
    override suspend fun deleteExam(exam: ExamEntity) {}
    override fun observeQuestionsByExam(examId: String): Flow<List<QuestionEntity>> = emptyFlow()
    override suspend fun getQuestionsByExam(examId: String): List<QuestionEntity> = emptyList()
    override suspend fun insertQuestions(questions: List<QuestionEntity>) {}
    override suspend fun insertQuestion(question: QuestionEntity) {}
    override suspend fun deleteQuestionsByExam(examId: String) {}
    override suspend fun getAnswersByExam(examId: String): List<PrivateAnswerEntity> = emptyList()
    override suspend fun getAnswerForQuestion(examId: String, qid: String): PrivateAnswerEntity? = null
    override suspend fun insertAnswers(answers: List<PrivateAnswerEntity>) {}
    override suspend fun deleteAnswersByExam(examId: String) {}
    override fun observeAllAccessRequests(): Flow<List<AccessRequestEntity>> = emptyFlow()
    override fun observeAccessRequestsByUser(uid: String): Flow<List<AccessRequestEntity>> = emptyFlow()
    override suspend fun getPendingRequest(uid: String, scopeId: String): AccessRequestEntity? = null
    override suspend fun getPendingRequestCount(): Int = 0
    override suspend fun insertAccessRequest(request: AccessRequestEntity) {}
    override suspend fun updateAccessRequest(request: AccessRequestEntity) {}
    override fun observeActivePlans(): Flow<List<MembershipPlanEntity>> = emptyFlow()
    override fun observeAllPlans(): Flow<List<MembershipPlanEntity>> = emptyFlow()
    override suspend fun getPlanById(id: String): MembershipPlanEntity? = null
    override suspend fun getActivePlanCount(): Int = 0
    override suspend fun insertPlan(plan: MembershipPlanEntity) {}
    override suspend fun updatePlan(plan: MembershipPlanEntity) {}
    override suspend fun deletePlan(plan: MembershipPlanEntity) {}
    override fun observeResultsByUser(uid: String): Flow<List<ExamResultEntity>> = emptyFlow()
    override suspend fun getResultsForExamByUser(uid: String, examId: String): List<ExamResultEntity> = emptyList()
    override suspend fun getResultById(id: String): ExamResultEntity? = null
    override suspend fun insertResult(result: ExamResultEntity) {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MedBankBulkImportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = MedBankRepository(context, FakeDao())

    @Test
    fun testBulkImportParsing_Success() {
        val input = """
EXAM|Cardiology Mock|Cardiology|licensing|usmle|60|100|0.25|plan_usmle|published
Q|What is the first-line drug for stable angina?|Aspirin & beta-blocker|Nitroglycerin only|Morphine|Digoxin|A|Beta-blockers reduce myocardial oxygen demand.|medium|cardio;ischemia
Q|Which diuretic acts on the loop of Henle?|Furosemide|Hydrochlorothiazide|Spironolactone|Mannitol|A|Furosemide inhibits the Na+/K+/2Cl- cotransporter.|easy|renal;pharm
        """.trimIndent()

        val result = repository.parseBulkImport(input)
        assertTrue(result.success)
        assertEquals(0, result.errors.size)
        assertNotNull(result.exam)
        assertEquals("Cardiology Mock", result.exam?.title)
        assertEquals(2, result.questions.size)
        assertEquals(2, result.answers.size)
        assertEquals(0, result.answers[0].correctIndex)
    }

    @Test
    fun testBulkImportParsing_InvalidHeader() {
        val input = """
NOT_AN_EXAM|Invalid
Q|Question text|A|B|C|D|A|Explanation|easy|tag
        """.trimIndent()

        val result = repository.parseBulkImport(input)
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("must start with EXAM") })
    }

    @Test
    fun testBulkImportParsing_InvalidCorrectOption() {
        val input = """
EXAM|Exam Title|Subject|licensing|usmle|60|100|0.25|plan_1|published
Q|Question text|A|B|C|D|Z|Explanation|easy|tag
        """.trimIndent()

        val result = repository.parseBulkImport(input)
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("Correct answer must be A, B, C, or D") })
    }
}
