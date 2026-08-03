package com.example.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AlarmSettingEntity
import com.example.data.ProtocolTask
import com.example.data.ProtocolTasks
import com.example.data.WaterEntryEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Theme Colors matching the Web Prototype's Slate & Emerald
val DeepSlateBg = Color(0xFF0F172A)
val SlateCardBg = Color(0xFF1E293B)
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF059669)
val BlueWater = Color(0xFF3B82F6)
val BlueWaterLight = Color(0xFF60A5FA)
val PurpleSleep = Color(0xFF8B5CF6)
val PurpleSleepLight = Color(0xFFA78BFA)
val TextGray = Color(0xFF9CA3AF)
val BorderGray = Color(0xFF374151)

enum class AppTab(val title: String, val icon: String) {
    TAREFAS("Tarefas", "✅"),
    ALARMES("Alarmes", "🔔"),
    AGUA("Água", "💧"),
    SONO("Sono", "😴"),
    HISTORICO("Histórico", "📊")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotinaApp(viewModel: RotinaViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(AppTab.TAREFAS) }

    // State bindings
    val tasks by viewModel.tasksForSelectedDay.collectAsStateWithLifecycle()
    val waterEntries by viewModel.waterEntriesForSelectedDay.collectAsStateWithLifecycle()
    val sleepRecord by viewModel.sleepRecordForSelectedDay.collectAsStateWithLifecycle()
    val alarms by viewModel.activeAlarms.collectAsStateWithLifecycle()
    val history by viewModel.historySummaries.collectAsStateWithLifecycle()

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDayOfWeek by viewModel.selectedDayOfWeek.collectAsStateWithLifecycle()

    // Notification permission check for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notificações ativadas com sucesso! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Ative as notificações para receber alarmes das tarefas.", Toast.LENGTH_LONG).show()
        }
    }

    // Main App Container with Linear Gradient Background
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Elegant M3 Style Bottom Navigation Bar customized to match dark theme
            NavigationBar(
                containerColor = SlateCardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            Text(text = tab.icon, fontSize = 20.sp)
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (activeTab == tab) EmeraldPrimary else TextGray,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.2f),
                            unselectedIconColor = TextGray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepSlateBg, Color(0xFF1E293B))
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Biological Banner Header
                HeaderBanner()

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Warning Card if permission is not granted
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    NotificationPermissionBanner {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic tab view switching
                Crossfade(targetState = activeTab, label = "TabTransition") { tab ->
                    when (tab) {
                        AppTab.TAREFAS -> ChecklistTab(
                            tasks = tasks,
                            selectedDate = selectedDate,
                            selectedDayOfWeek = selectedDayOfWeek,
                            onDateChanged = { viewModel.setSelectedDate(it) },
                            onTaskComplete = { taskId, time, notes ->
                                viewModel.completeTask(taskId, time, notes)
                            },
                            onTaskUncomplete = { viewModel.uncompleteTask(it) },
                            onSetAlarm = { taskId, time, active ->
                                viewModel.setAlarm(taskId, time, active)
                            },
                            alarms = alarms
                        )
                        AppTab.ALARMES -> AlarmsTab(
                            alarms = alarms,
                            onCancelAlarm = { viewModel.cancelAlarm(it) }
                        )
                        AppTab.AGUA -> WaterTab(
                            waterEntries = waterEntries,
                            onAddWater = { viewModel.addWater(it) },
                            onDeleteEntry = { viewModel.deleteWaterEntry(it) }
                        )
                        AppTab.SONO -> SleepTab(
                            currentDate = selectedDate,
                            sleepRecord = sleepRecord,
                            onSaveSleep = { bedtime, sleepTime, wakeTime, notes ->
                                viewModel.saveSleepRecord(bedtime, sleepTime, wakeTime, notes)
                            }
                        )
                        AppTab.HISTORICO -> HistoryTab(
                            historySummaries = history,
                            onSelectDate = {
                                viewModel.setSelectedDate(it)
                                activeTab = AppTab.TAREFAS
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                FooterSection()
            }
        }
    }
}

@Composable
fun HeaderBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ROTINA SAUDÁVEL",
                fontSize = 22.sp,
                style = LocalTextStyle.current.copy(
                    brush = Brush.horizontalGradient(listOf(EmeraldPrimary, EmeraldLight)),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = "Mês 1 (Agosto) - Foco em Redução de Cortisol",
                color = TextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.1f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = "🧬", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Protocolo de Otimização Biológica",
                    color = EmeraldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NotificationPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)),
        border = BorderStroke(2.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔔 Ativar Notificações",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Receba alertas nos horários das suas tarefas para não esquecer nada!",
                color = TextGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("notification_permission_button")
            ) {
                Text("🔔 Ativar Notificações", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// TAREFAS TAB (CHECKLIST)
// -------------------------------------------------------------
@Composable
fun ChecklistTab(
    tasks: List<UIProtocolTask>,
    selectedDate: String,
    selectedDayOfWeek: String,
    onDateChanged: (String) -> Unit,
    onTaskComplete: (taskId: String, time: String, notes: String) -> Unit,
    onTaskUncomplete: (taskId: String) -> Unit,
    onSetAlarm: (taskId: String, time: String, active: Boolean) -> Unit,
    alarms: List<AlarmSettingEntity>
) {
    val context = LocalContext.current
    val completedCount = tasks.filter { it.progress?.completed == true }.size
    val totalCount = tasks.size
    val progressPercent = if (totalCount > 0) (completedCount.toFloat() / totalCount) * 100 else 0f

    // Selected task for logging dialog
    var taskToLog by remember { mutableStateOf<ProtocolTask?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Canvas Progress Ring
        ProgressRingCanvas(
            completedCount = completedCount,
            totalCount = totalCount,
            dayName = selectedDayOfWeek,
            progressPercent = progressPercent
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Day Selector Button (Simulates calendar chooser)
        DaySelectorDropdown(
            selectedDate = selectedDate,
            selectedDayName = selectedDayOfWeek,
            onDateSelected = onDateChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Linear Progress Bar matching HTML's bar
        LinearProgressTracker(progressPercent = progressPercent)

        Spacer(modifier = Modifier.height(16.dp))

        // Status Card
        StatusIndicatorCard(completedCount = completedCount, totalCount = totalCount)

        Spacer(modifier = Modifier.height(12.dp))

        // Tasks List
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tasks.forEach { uiTask ->
                val task = uiTask.task
                val isCompleted = uiTask.progress?.completed == true
                val alarmSetting = alarms.find { it.taskId == task.id }
                val isAlarmActive = alarmSetting?.active == true

                TaskRowItem(
                    task = task,
                    isCompleted = isCompleted,
                    completedTime = uiTask.progress?.time,
                    completedNotes = uiTask.progress?.notes,
                    isAlarmActive = isAlarmActive,
                    alarmTime = alarmSetting?.time ?: task.targetTime ?: "12:00",
                    onToggle = {
                        if (isCompleted) {
                            onTaskUncomplete(task.id)
                        } else {
                            taskToLog = task
                        }
                    },
                    onToggleAlarm = { time, active ->
                        onSetAlarm(task.id, time, active)
                    }
                )
            }
        }
    }

    // Modal dialog to input time and notes for task completion
    if (taskToLog != null) {
        val currentTask = taskToLog!!
        var completedTime by remember {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            mutableStateOf(sdf.format(Date()))
        }
        var observations by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { taskToLog = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                border = BorderStroke(2.dp, BorderGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Registrar: ${currentTask.icon} ${currentTask.text}",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Horário realizado:",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Time Picker triggering native dialogue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepSlateBg)
                            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            .clickable {
                                val parts = completedTime.split(":")
                                val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m ->
                                    completedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                }, initialH, initialM, true).show()
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = completedTime, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Pick Time", tint = EmeraldPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Observações:",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        placeholder = { Text("Ex: Me senti revigorado...", color = TextGray.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DeepSlateBg,
                            unfocusedContainerColor = DeepSlateBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { taskToLog = null },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderGray),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancelar", color = Color.White)
                        }

                        Button(
                            onClick = {
                                onTaskComplete(currentTask.id, completedTime, observations.trim())
                                taskToLog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("confirm_task_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressRingCanvas(
    completedCount: Int,
    totalCount: Int,
    dayName: String,
    progressPercent: Float
) {
    // Elegant radial Canvas drawing
    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 1. Background circle track
            drawCircle(
                color = Color(0xFF1E293B),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Animated Progress Arc
            val sweepAngle = (progressPercent / 100f) * 360f
            drawArc(
                brush = Brush.linearGradient(listOf(EmeraldPrimary, EmeraldLight)),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Task Beads/Dots around the perimeter
            if (totalCount > 0) {
                for (i in 0 until totalCount) {
                    val angle = (i.toFloat() / totalCount) * 2 * Math.PI - Math.PI / 2
                    val beadRadius = radius + 15.dp.toPx()
                    val dotX = center.x + beadRadius * cos(angle).toFloat()
                    val dotY = center.y + beadRadius * sin(angle).toFloat()

                    val isBeadCompleted = i < completedCount
                    drawCircle(
                        color = if (isBeadCompleted) EmeraldPrimary else BorderGray,
                        radius = 4.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }
        }

        // Inner stats textual display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${progressPercent.toInt()}%",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dayName,
                color = TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DaySelectorDropdown(
    selectedDate: String,
    selectedDayName: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Row of week options (Simulates selecting calendar)
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "📅 Selecionar Dia da Semana:",
            color = TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SlateCardBg)
                .border(2.dp, BorderGray, RoundedCornerShape(12.dp))
                .clickable {
                    // Trigger Android standard DatePickerDialog
                    val dialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            onDateSelected(format.format(cal.time))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    dialog.show()
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🗓️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$selectedDayName ($selectedDate)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Selecionar Data",
                tint = EmeraldPrimary
            )
        }
    }
}

@Composable
fun LinearProgressTracker(progressPercent: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCardBg)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📊 Progresso do dia", color = Color.White, fontSize = 13.sp)
            Text(
                text = "${progressPercent.toInt()}%",
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shimmering Linear Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(DeepSlateBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressPercent / 100f)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(EmeraldDark, EmeraldPrimary, EmeraldLight)
                        )
                    )
            )
        }
    }
}

@Composable
fun StatusIndicatorCard(completedCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    completedCount == totalCount && totalCount > 0 -> "🎉 Todas as tarefas concluídas!"
                    completedCount == 0 -> "📋 Nenhuma tarefa concluída ainda"
                    else -> "✅ $completedCount de $totalCount tarefas concluídas"
                },
                color = if (completedCount == totalCount) EmeraldPrimary else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TaskRowItem(
    task: ProtocolTask,
    isCompleted: Boolean,
    completedTime: String?,
    completedNotes: String?,
    isAlarmActive: Boolean,
    alarmTime: String,
    onToggle: () -> Unit,
    onToggleAlarm: (time: String, active: Boolean) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) EmeraldPrimary.copy(alpha = 0.08f) else SlateCardBg.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            2.dp,
            if (isCompleted) EmeraldPrimary.copy(alpha = 0.3f) else BorderGray
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task Leading Icon
            Text(
                text = task.icon,
                fontSize = 22.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Dynamic Checkbox with Tick
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCompleted) EmeraldPrimary else Color.Transparent)
                    .border(
                        2.dp,
                        if (isCompleted) EmeraldPrimary else TextGray,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Label & Timestamp Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.text,
                    color = if (isCompleted) TextGray else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (isCompleted && completedTime != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✅ Realizado às $completedTime",
                            color = EmeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!completedNotes.isNullOrEmpty()) {
                            Text(
                                text = " - $completedNotes",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (task.targetTime != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🎯 Alvo: ${task.targetTime}",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Alarm Clock setting trigger
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isCompleted) {
                            Toast
                                .makeText(
                                    context,
                                    "Esta tarefa já foi concluída hoje!",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        } else {
                            if (isAlarmActive) {
                                onToggleAlarm(alarmTime, false)
                                Toast
                                    .makeText(
                                        context,
                                        "Alarme desativado!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            } else {
                                // Trigger Native clock pick to schedule
                                val parts = alarmTime.split(":")
                                val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m ->
                                    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    onToggleAlarm(formattedTime, true)
                                    Toast
                                        .makeText(
                                            context,
                                            "Alarme agendado para as $formattedTime!",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }, initialH, initialM, true).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAlarmActive) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = "Task Alarm",
                    tint = if (isAlarmActive) Color(0xFFFBBF24) else TextGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// ALARMES TAB (SCHEDULED ALARMS)
// -------------------------------------------------------------
@Composable
fun AlarmsTab(
    alarms: List<AlarmSettingEntity>,
    onCancelAlarm: (taskId: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🔔", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alarmes Configurados",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (alarms.isEmpty()) {
                Text(
                    text = "Nenhum alarme configurado para hoje.",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    alarms.forEach { alarm ->
                        val task = ProtocolTasks.findTask(alarm.taskId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepSlateBg)
                                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⏰ ${alarm.time}",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${task?.icon ?: ""} ${task?.text ?: "Tarefa"}",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { onCancelAlarm(alarm.taskId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Cancelar", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ÁGUA TAB (WATER LOG)
// -------------------------------------------------------------
@Composable
fun WaterTab(
    waterEntries: List<WaterEntryEntity>,
    onAddWater: (Int) -> Unit,
    onDeleteEntry: (Int) -> Unit
) {
    var customAmount by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    val consumedTotal = waterEntries.sumOf { it.amount }
    val goal = 3000
    val waterProgress = if (consumedTotal >= goal) 1f else consumedTotal.toFloat() / goal
    val remaining = if (consumedTotal >= goal) 0 else goal - consumedTotal

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BlueWater.copy(alpha = 0.05f)),
        border = BorderStroke(2.dp, BlueWater.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💧", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Hidratação Diária", color = BlueWaterLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(text = "Meta: 3000ml", color = TextGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Water Tracker progress cylinder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(DeepSlateBg)
                    .border(2.dp, BorderGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(waterProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(BlueWater, BlueWaterLight, Color(0xFF93C5FD))
                            )
                        )
                        .align(Alignment.CenterStart)
                )

                Text(
                    text = "$consumedTotal / ${goal}ml",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Consumido: $consumedTotal ml",
                    color = BlueWaterLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Faltam: $remaining ml",
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logging Quick buttons grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onAddWater(200) },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueWater.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BlueWater),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+200ml", color = BlueWaterLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddWater(300) },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueWater.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BlueWater),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+300ml", color = BlueWaterLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onAddWater(500) },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueWater.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, BlueWater),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+500ml", color = BlueWaterLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom amount toggle Button
            Button(
                onClick = { showCustomInput = !showCustomInput },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleSleep.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, PurpleSleep),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💧 Personalizado", color = PurpleSleepLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            if (showCustomInput) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Valor em ml", color = TextGray.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleSleep,
                            unfocusedBorderColor = BorderGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DeepSlateBg,
                            unfocusedContainerColor = DeepSlateBg
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val amount = customAmount.toIntOrNull()
                            if (amount != null && amount > 0) {
                                onAddWater(amount)
                                customAmount = ""
                                showCustomInput = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleSleep),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_custom_water_button")
                    ) {
                        Text("Adicionar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Water history entries
            Text(
                text = "Histórico de hoje:",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (waterEntries.isEmpty()) {
                Text(
                    text = "Nenhum consumo registrado hoje.",
                    color = TextGray.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    waterEntries.reversed().forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DeepSlateBg)
                                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🕐 ${entry.time}", color = BlueWaterLight, fontSize = 13.sp)
                            Text(text = "+${entry.amount}ml", color = EmeraldLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { onDeleteEntry(entry.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover registro",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SONO TAB (SLEEP TRACKER)
// -------------------------------------------------------------
@Composable
fun SleepTab(
    currentDate: String,
    sleepRecord: com.example.data.SleepEntity?,
    onSaveSleep: (bedtime: String, sleepTime: String, wakeTime: String, notes: String) -> Unit
) {
    val context = LocalContext.current

    var bedtime by remember { mutableStateOf(sleepRecord?.bedtime ?: "22:00") }
    var sleepTime by remember { mutableStateOf(sleepRecord?.sleepTime ?: "22:15") }
    var wakeTime by remember { mutableStateOf(sleepRecord?.wakeTime ?: "06:30") }
    var notes by remember { mutableStateOf(sleepRecord?.notes ?: "") }

    // Sync state when record updates in DB
    LaunchedEffect(sleepRecord) {
        if (sleepRecord != null) {
            bedtime = sleepRecord.bedtime
            sleepTime = sleepRecord.sleepTime
            wakeTime = sleepRecord.wakeTime
            notes = sleepRecord.notes ?: ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurpleSleep.copy(alpha = 0.05f)),
        border = BorderStroke(2.dp, PurpleSleep.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "😴 Registro de Sono - $currentDate",
                color = PurpleSleepLight,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time Picking Inputs
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bedtime Input row
                SleepTimeRow(
                    label = "🛏️ Deitou às:",
                    timeValue = bedtime,
                    onPickTime = {
                        val parts = bedtime.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, hour, min ->
                            bedtime = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
                        }, h, m, true).show()
                    }
                )

                // SleepTime Input row
                SleepTimeRow(
                    label = "💤 Dormiu às:",
                    timeValue = sleepTime,
                    onPickTime = {
                        val parts = sleepTime.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 15
                        TimePickerDialog(context, { _, hour, min ->
                            sleepTime = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
                        }, h, m, true).show()
                    }
                )

                // WakeTime Input row
                SleepTimeRow(
                    label = "⏰ Acordou às:",
                    timeValue = wakeTime,
                    onPickTime = {
                        val parts = wakeTime.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
                        TimePickerDialog(context, { _, hour, min ->
                            wakeTime = String.format(Locale.getDefault(), "%02d:%02d", hour, min)
                        }, h, m, true).show()
                    }
                )

                // Sleep Notes Input
                Text(
                    text = "📝 Notas:",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Ex: Acordei uma vez na noite...", color = TextGray.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleSleep,
                        unfocusedBorderColor = BorderGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DeepSlateBg,
                        unfocusedContainerColor = DeepSlateBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Sleep button
            Button(
                onClick = {
                    onSaveSleep(bedtime, sleepTime, wakeTime, notes.trim())
                    Toast.makeText(context, "Registro de sono salvo! 💾", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleSleep),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_sleep_button")
            ) {
                Text("💾 Salvar Registro", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Results Display Card if there is recorded sleep data
            if (sleepRecord != null) {
                Spacer(modifier = Modifier.height(20.dp))
                SleepAnalysisCard(record = sleepRecord)
            }
        }
    }
}

@Composable
fun SleepTimeRow(
    label: String,
    timeValue: String,
    onPickTime: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp)
        Row(
            modifier = Modifier
                .width(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DeepSlateBg)
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                .clickable { onPickTime() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = timeValue, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Picker",
                tint = PurpleSleepLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SleepAnalysisCard(record: com.example.data.SleepEntity) {
    val latencyClassColor = when {
        record.latencyMin > 30 -> Color(0xFFEF4444)
        record.latencyMin > 20 -> Color(0xFFF59E0B)
        else -> EmeraldLight
    }

    val latencyMsg = when {
        record.latencyMin > 30 -> "Muito alto! 😟 Tente relaxar mais."
        record.latencyMin > 20 -> "Um pouco alto ⚠️ Reduza luz azul."
        else -> "Excelente! 🌟"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSlateBg),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "📊 Análise do Sono",
                color = PurpleSleepLight,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sleep Latency display
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "⏱️ Latência:", color = TextGray, fontSize = 12.sp)
                    Text(
                        text = "${record.latencyMin} min - $latencyMsg",
                        color = latencyClassColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sleep Duration display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "😴 Duração:", color = TextGray, fontSize = 12.sp)
                    Text(
                        text = record.duration,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Saved Bedtime/Sleep hours check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "🛏️ Deitou / Dormiu:", color = TextGray, fontSize = 12.sp)
                    Text(
                        text = "${record.bedtime} / ${record.sleepTime}",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                if (!record.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = BorderGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📝 Notas: ${record.notes}",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HISTÓRICO TAB (AGGREGATED HISTORY)
// -------------------------------------------------------------
@Composable
fun HistoryTab(
    historySummaries: List<HistorySummary>,
    onSelectDate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📊", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Histórico de Registros",
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historySummaries.isEmpty()) {
            Text(
                text = "Nenhum histórico registrado ainda. Conclua tarefas, registre água ou durma para gerar o histórico!",
                color = TextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                historySummaries.forEach { summary ->
                    HistoryCardItem(
                        summary = summary,
                        onClick = { onSelectDate(summary.date) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCardItem(
    summary: HistorySummary,
    onClick: () -> Unit
) {
    // Beautiful formatted weekday and month
    val dateFormatted = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = inputFormat.parse(summary.date) ?: Date()
        val outputFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        outputFormat.format(date).capitalize(Locale.getDefault())
    } catch (e: Exception) {
        "${summary.dayOfWeek}, ${summary.date}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SlateCardBg.copy(alpha = 0.6f)),
        border = BorderStroke(1.5.dp, BorderGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "📅 $dateFormatted",
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats grid representing checklist completion, water, and sleep
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tasks summary block
                val taskPercent = if (summary.totalTasksCount > 0) {
                    (summary.tasksCompletedCount.toFloat() / summary.totalTasksCount) * 100
                } else {
                    0f
                }
                HistoryStatBlock(
                    label = "Tarefas",
                    value = "${taskPercent.toInt()}%",
                    icon = "✅",
                    modifier = Modifier.weight(1f)
                )

                // Water summary block
                HistoryStatBlock(
                    label = "Água",
                    value = "${summary.waterConsumed}ml",
                    icon = "💧",
                    modifier = Modifier.weight(1f)
                )

                // Sleep summary block
                HistoryStatBlock(
                    label = "Sono",
                    value = summary.sleepDuration ?: "--",
                    icon = "😴",
                    modifier = Modifier.weight(1f)
                )
            }

            // Sleep latency footnote details if available
            if (summary.sleepLatency != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🛏️ Latência: ${summary.sleepLatency} min",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    if (!summary.sleepNotes.isNullOrEmpty()) {
                        Text(
                            text = " | 📝 ${summary.sleepNotes}",
                            color = TextGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryStatBlock(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DeepSlateBg)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = EmeraldPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = label,
            color = TextGray,
            fontSize = 10.sp
        )
    }
}

// -------------------------------------------------------------
// FOOTER & DECORATIVE SECTIONS
// -------------------------------------------------------------
@Composable
fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(color = BorderGray, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "🧠 Protocolo baseado em neurociência e cronobiologia",
            color = TextGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Mantenha consistência para otimizar seu ciclo circadiano",
            color = TextGray.copy(alpha = 0.8f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}
