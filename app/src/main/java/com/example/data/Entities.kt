package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_progress", primaryKeys = ["date", "taskId"])
data class TaskProgressEntity(
    val date: String,       // format: "yyyy-MM-dd"
    val taskId: String,     // e.g. "seg1", "ter1", etc.
    val completed: Boolean,
    val time: String? = null,      // e.g. "04:15"
    val notes: String? = null,     // e.g. "Sentindo um pouco de sono"
    val timestamp: Long? = null
)

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,       // format: "yyyy-MM-dd"
    val amount: Int,        // in ml
    val time: String,       // format: "HH:mm"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_records")
data class SleepEntity(
    @PrimaryKey val date: String, // format: "yyyy-MM-dd"
    val bedtime: String,          // format: "HH:mm"
    val sleepTime: String,        // format: "HH:mm"
    val wakeTime: String,         // format: "HH:mm"
    val notes: String? = null,
    val latencyMin: Int,          // delay between bed time and sleeping in minutes
    val duration: String,         // formatted duration e.g. "7h30min"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarm_settings")
data class AlarmSettingEntity(
    @PrimaryKey val taskId: String,
    val time: String,             // format: "HH:mm"
    val active: Boolean
)
