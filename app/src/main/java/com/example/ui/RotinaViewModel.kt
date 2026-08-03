package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.receiver.AlarmSchedulerHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class UIProtocolTask(
    val task: ProtocolTask,
    val progress: TaskProgressEntity?
)

data class HistorySummary(
    val date: String,
    val dayOfWeek: String,
    val tasksCompletedCount: Int,
    val totalTasksCount: Int,
    val waterConsumed: Int,
    val sleepDuration: String?,
    val sleepLatency: Int?,
    val sleepNotes: String?
)

class RotinaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RotinaRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RotinaRepository(db)
    }

    // Selected Date & Weekday State
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedDayOfWeek = MutableStateFlow(getPortugueseWeekday(Calendar.getInstance()))
    val selectedDayOfWeek: StateFlow<String> = _selectedDayOfWeek.asStateFlow()

    // Active Alarms from DB
    val activeAlarms: StateFlow<List<AlarmSettingEntity>> = repository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactively load and merge tasks for the selected day and date
    val tasksForSelectedDay: StateFlow<List<UIProtocolTask>> = combine(
        _selectedDate,
        _selectedDayOfWeek
    ) { date, dayOfWeek ->
        Pair(date, dayOfWeek)
    }.flatMapLatest { (date, dayOfWeek) ->
        val staticTasks = ProtocolTasks.rotinas[dayOfWeek] ?: emptyList()
        repository.getTaskProgressForDate(date).map { progressList ->
            staticTasks.map { task ->
                val progress = progressList.find { it.taskId == task.id }
                UIProtocolTask(task, progress)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactively load water entries for selected date
    val waterEntriesForSelectedDay: StateFlow<List<WaterEntryEntity>> = _selectedDate
        .flatMapLatest { date ->
            repository.getWaterEntriesForDate(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactively load sleep record for selected date
    val sleepRecordForSelectedDay: StateFlow<SleepEntity?> = _selectedDate
        .flatMapLatest { date ->
            repository.getSleepRecordForDate(date)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Reactively load and aggregate history across all dates
    val historySummaries: StateFlow<List<HistorySummary>> = combine(
        repository.getAllProgress(),
        repository.getAllWaterEntries(),
        repository.getAllSleepRecords()
    ) { progressList, waterEntries, sleepRecords ->
        val dates = (progressList.map { it.date } + waterEntries.map { it.date } + sleepRecords.map { it.date }).toSet()

        dates.map { date ->
            val dateProgress = progressList.filter { it.date == date }
            val dateWater = waterEntries.filter { it.date == date }
            val dateSleep = sleepRecords.find { it.date == date }

            val dayOfWeek = getPortugueseWeekdayForDateString(date)
            val staticTasks = ProtocolTasks.rotinas[dayOfWeek] ?: emptyList()
            val totalTasks = staticTasks.size
            val completedCount = dateProgress.filter { it.completed }.size

            HistorySummary(
                date = date,
                dayOfWeek = dayOfWeek,
                tasksCompletedCount = completedCount,
                totalTasksCount = totalTasks,
                waterConsumed = dateWater.sumOf { it.amount },
                sleepDuration = dateSleep?.duration,
                sleepLatency = dateSleep?.latencyMin,
                sleepNotes = dateSleep?.notes
            )
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Actions
    fun setSelectedDay(dayName: String) {
        _selectedDayOfWeek.value = dayName
    }

    fun setSelectedDate(dateString: String) {
        _selectedDate.value = dateString
        _selectedDayOfWeek.value = getPortugueseWeekdayForDateString(dateString)
    }

    fun completeTask(taskId: String, completedTime: String, notes: String?) {
        viewModelScope.launch {
            val progress = TaskProgressEntity(
                date = _selectedDate.value,
                taskId = taskId,
                completed = true,
                time = completedTime,
                notes = notes,
                timestamp = System.currentTimeMillis()
            )
            repository.insertTaskProgress(progress)

            // Cancel any pending physical alarm when a task gets marked as completed
            AlarmSchedulerHelper.cancelAlarm(getApplication(), taskId)
        }
    }

    fun uncompleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTaskProgress(_selectedDate.value, taskId)

            // If an alarm is configured in db for this task, reschedule it!
            val alarm = repository.getAlarmByTaskId(taskId)
            if (alarm != null && alarm.active) {
                val task = ProtocolTasks.findTask(taskId)
                if (task != null) {
                    AlarmSchedulerHelper.scheduleAlarm(
                        getApplication(),
                        taskId,
                        task.text,
                        task.icon,
                        alarm.time
                    )
                }
            }
        }
    }

    fun addWater(amount: Int) {
        viewModelScope.launch {
            val entry = WaterEntryEntity(
                date = _selectedDate.value,
                amount = amount,
                time = getCurrentTimeString()
            )
            repository.insertWaterEntry(entry)
        }
    }

    fun deleteWaterEntry(entryId: Int) {
        viewModelScope.launch {
            repository.deleteWaterEntryById(entryId)
        }
    }

    fun saveSleepRecord(bedtime: String, sleepTime: String, wakeTime: String, notes: String?) {
        viewModelScope.launch {
            val (latency, duration) = calculateSleepStats(bedtime, sleepTime, wakeTime)
            val record = SleepEntity(
                date = _selectedDate.value,
                bedtime = bedtime,
                sleepTime = sleepTime,
                wakeTime = wakeTime,
                notes = notes,
                latencyMin = latency,
                duration = duration
            )
            repository.insertSleepRecord(record)
        }
    }

    fun setAlarm(taskId: String, time: String, active: Boolean) {
        viewModelScope.launch {
            val alarm = AlarmSettingEntity(taskId, time, active)
            repository.insertAlarm(alarm)

            val task = ProtocolTasks.findTask(taskId)
            if (task != null) {
                if (active) {
                    AlarmSchedulerHelper.scheduleAlarm(
                        getApplication(),
                        taskId,
                        task.text,
                        task.icon,
                        time
                    )
                } else {
                    AlarmSchedulerHelper.cancelAlarm(getApplication(), taskId)
                }
            }
        }
    }

    fun cancelAlarm(taskId: String) {
        viewModelScope.launch {
            repository.deleteAlarmByTaskId(taskId)
            AlarmSchedulerHelper.cancelAlarm(getApplication(), taskId)
        }
    }

    // Date Helpers
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getPortugueseWeekday(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Segunda"
            Calendar.TUESDAY -> "Terça"
            Calendar.WEDNESDAY -> "Quarta"
            Calendar.THURSDAY -> "Quinta"
            Calendar.FRIDAY -> "Sexta"
            Calendar.SATURDAY -> "Sábado"
            else -> "Segunda"
        }
    }

    private fun getPortugueseWeekdayForDateString(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString) ?: return "Segunda"
            val calendar = Calendar.getInstance().apply { time = date }
            getPortugueseWeekday(calendar)
        } catch (e: Exception) {
            "Segunda"
        }
    }

    private fun calculateSleepStats(bedtime: String, sleepTime: String, wakeTime: String): Pair<Int, String> {
        val bedParts = bedtime.split(":")
        val sleepParts = sleepTime.split(":")
        val wakeParts = wakeTime.split(":")

        val bedH = bedParts[0].toIntOrNull() ?: 0
        val bedM = bedParts[1].toIntOrNull() ?: 0
        val sleepH = sleepParts[0].toIntOrNull() ?: 0
        val sleepM = sleepParts[1].toIntOrNull() ?: 0
        val wakeH = wakeParts[0].toIntOrNull() ?: 0
        val wakeM = wakeParts[1].toIntOrNull() ?: 0

        val bedDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bedH)
            set(Calendar.MINUTE, bedM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sleepDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, sleepH)
            set(Calendar.MINUTE, sleepM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val wakeDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeH)
            set(Calendar.MINUTE, wakeM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (sleepDate.before(bedDate)) {
            sleepDate.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (wakeDate.before(sleepDate)) {
            wakeDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val latencyMs = sleepDate.timeInMillis - bedDate.timeInMillis
        val latencyMin = (latencyMs / 60000).toInt()

        val durationMs = wakeDate.timeInMillis - sleepDate.timeInMillis
        val durationHours = durationMs / 3600000
        val durationMins = (durationMs % 3600000) / 60000

        val durationStr = "${durationHours}h${durationMins}min"
        return Pair(latencyMin, durationStr)
    }
}
