package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseSyncService {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Firebase Firestore not initialized: ${e.message}")
            null
        }
    }

    suspend fun syncTaskProgress(entity: TaskProgressEntity) = withContext(Dispatchers.IO) {
        try {
            val firestore = db ?: return@withContext
            val docId = "${entity.date}_${entity.taskId}"
            val data = hashMapOf(
                "date" to entity.date,
                "taskId" to entity.taskId,
                "completed" to entity.completed,
                "time" to (entity.time ?: ""),
                "notes" to (entity.notes ?: ""),
                "timestamp" to (entity.timestamp ?: System.currentTimeMillis()),
                "deviceCloudSync" to true
            )
            firestore.collection("rotina_task_progress")
                .document(docId)
                .set(data)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing task to Firestore: ${e.message}")
        }
    }

    suspend fun syncSleepRecord(entity: SleepEntity) = withContext(Dispatchers.IO) {
        try {
            val firestore = db ?: return@withContext
            val data = hashMapOf(
                "date" to entity.date,
                "bedtime" to entity.bedtime,
                "sleepTime" to entity.sleepTime,
                "wakeTime" to entity.wakeTime,
                "notes" to (entity.notes ?: ""),
                "latencyMin" to entity.latencyMin,
                "duration" to entity.duration,
                "timestamp" to entity.timestamp,
                "deviceCloudSync" to true
            )
            firestore.collection("rotina_sleep_records")
                .document(entity.date)
                .set(data)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing sleep to Firestore: ${e.message}")
        }
    }

    suspend fun syncWaterEntry(entity: WaterEntryEntity) = withContext(Dispatchers.IO) {
        try {
            val firestore = db ?: return@withContext
            val docId = "${entity.date}_${entity.id}"
            val data = hashMapOf(
                "date" to entity.date,
                "amount" to entity.amount,
                "time" to entity.time,
                "timestamp" to entity.timestamp,
                "deviceCloudSync" to true
            )
            firestore.collection("rotina_water_entries")
                .document(docId)
                .set(data)
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error syncing water to Firestore: ${e.message}")
        }
    }
}
