package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_progress WHERE date = :date")
    fun getTaskProgressForDate(date: String): Flow<List<TaskProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskProgress(progress: TaskProgressEntity)

    @Query("DELETE FROM task_progress WHERE date = :date AND taskId = :taskId")
    suspend fun deleteTaskProgress(date: String, taskId: String)

    @Query("SELECT * FROM task_progress ORDER BY date DESC")
    fun getAllProgress(): Flow<List<TaskProgressEntity>>
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterEntryById(id: Int)

    @Query("SELECT * FROM water_entries ORDER BY date DESC")
    fun getAllWaterEntries(): Flow<List<WaterEntryEntity>>
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records WHERE date = :date LIMIT 1")
    fun getSleepRecordForDate(date: String): Flow<SleepEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(sleep: SleepEntity)

    @Query("SELECT * FROM sleep_records ORDER BY date DESC")
    fun getAllSleepRecords(): Flow<List<SleepEntity>>
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm_settings")
    fun getAllAlarms(): Flow<List<AlarmSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmSettingEntity)

    @Query("DELETE FROM alarm_settings WHERE taskId = :taskId")
    suspend fun deleteAlarmByTaskId(taskId: String)

    @Query("SELECT * FROM alarm_settings WHERE taskId = :taskId LIMIT 1")
    suspend fun getAlarmByTaskId(taskId: String): AlarmSettingEntity?
}

@Database(
    entities = [
        TaskProgressEntity::class,
        WaterEntryEntity::class,
        SleepEntity::class,
        AlarmSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun waterDao(): WaterDao
    abstract fun sleepDao(): SleepDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rotina_saudavel_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
