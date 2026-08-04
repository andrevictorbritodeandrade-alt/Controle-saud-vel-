package com.example.data

import kotlinx.coroutines.flow.Flow

class RotinaRepository(private val database: AppDatabase) {
    private val taskDao = database.taskDao()
    private val waterDao = database.waterDao()
    private val sleepDao = database.sleepDao()
    private val alarmDao = database.alarmDao()
    private val firebaseSyncService = FirebaseSyncService()

    // Tasks
    fun getTaskProgressForDate(date: String): Flow<List<TaskProgressEntity>> =
        taskDao.getTaskProgressForDate(date)

    suspend fun insertTaskProgress(progress: TaskProgressEntity) {
        taskDao.insertTaskProgress(progress)
        firebaseSyncService.syncTaskProgress(progress)
    }

    suspend fun deleteTaskProgress(date: String, taskId: String) =
        taskDao.deleteTaskProgress(date, taskId)

    fun getAllProgress(): Flow<List<TaskProgressEntity>> =
        taskDao.getAllProgress()

    // Water
    fun getWaterEntriesForDate(date: String): Flow<List<WaterEntryEntity>> =
        waterDao.getWaterEntriesForDate(date)

    suspend fun insertWaterEntry(entry: WaterEntryEntity) {
        waterDao.insertWaterEntry(entry)
        firebaseSyncService.syncWaterEntry(entry)
    }

    suspend fun deleteWaterEntryById(id: Int) =
        waterDao.deleteWaterEntryById(id)

    fun getAllWaterEntries(): Flow<List<WaterEntryEntity>> =
        waterDao.getAllWaterEntries()

    // Sleep
    fun getSleepRecordForDate(date: String): Flow<SleepEntity?> =
        sleepDao.getSleepRecordForDate(date)

    suspend fun insertSleepRecord(sleep: SleepEntity) {
        sleepDao.insertSleepRecord(sleep)
        firebaseSyncService.syncSleepRecord(sleep)
    }

    fun getAllSleepRecords(): Flow<List<SleepEntity>> =
        sleepDao.getAllSleepRecords()

    // Alarms
    fun getAllAlarms(): Flow<List<AlarmSettingEntity>> =
        alarmDao.getAllAlarms()

    suspend fun insertAlarm(alarm: AlarmSettingEntity) =
        alarmDao.insertAlarm(alarm)

    suspend fun deleteAlarmByTaskId(taskId: String) =
        alarmDao.deleteAlarmByTaskId(taskId)

    suspend fun getAlarmByTaskId(taskId: String): AlarmSettingEntity? =
        alarmDao.getAlarmByTaskId(taskId)
}
