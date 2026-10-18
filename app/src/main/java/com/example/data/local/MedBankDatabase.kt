package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        CnicIndexEntity::class,
        SettingsEntity::class,
        ProgramEntity::class,
        MbbsYearEntity::class,
        LicensingPathwayEntity::class,
        SubjectEntity::class,
        ExamEntity::class,
        QuestionEntity::class,
        PrivateAnswerEntity::class,
        AccessRequestEntity::class,
        MembershipPlanEntity::class,
        ExamResultEntity::class,
        UserDeviceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MedBankDatabase : RoomDatabase() {
    abstract fun dao(): MedBankDao

    companion object {
        @Volatile
        private var INSTANCE: MedBankDatabase? = null

        fun getInstance(context: Context): MedBankDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedBankDatabase::class.java,
                    "medbank_master.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
