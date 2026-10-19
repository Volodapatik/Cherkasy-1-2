package ua.cherkasy.outage.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.cherkasy.outage.data.OutageEntity
import ua.cherkasy.outage.data.OutageRepository
import ua.cherkasy.outage.fcm.FcmTokenStore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutageScreen() {
    val context = LocalContext.current
    val repo = remember { OutageRepository(context) }
    val schedules by repo.getAllSchedules().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var statusHint by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            lastError = null
            statusHint = null
            try {
                val added = withContext(Dispatchers.IO) {
                    repo.checkForNewSchedules()
                }
                statusHint = if (added > 0) {
                    "Додано нових: $added"
                } else {
                    "Перевірено. Нових записів немає (або вже збережено)"
                }
            } catch (e: Exception) {
                lastError = "Помилка: ${e.message ?: e.javaClass.simpleName}"
            }
            isRefreshing = false
        }
    }

    fun copyToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result else FcmTokenStore.get(context)
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Токен ще не готовий", Toast.LENGTH_SHORT).show()
                return@addOnCompleteListener
            }
            FcmTokenStore.save(context, token)
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("fcm", token))
            Toast.makeText(context, "FCM токен скопійовано", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val latest = schedules.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Черга 1.2 — Черкасиобленерго",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { copyToken() }
                            )
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { refresh() },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Оновити",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ЧЕРГА 1.2",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (latest != null) {
                        Text(
                            text = latest.dateText.uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latest.timesText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Оновлено: ${formatTime(latest.publishedAt)}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = if (isRefreshing) {
                                "Завантаження графіка…"
                            } else {
                                "Ще немає даних.\nТягнемо з каналу автоматично…"
                            },
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (isRefreshing) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (lastError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = lastError!!, color = Color.Red, fontSize = 13.sp)
            } else if (statusHint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = statusHint!!, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Історія графіків",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (schedules.isEmpty()) {
                Text(
                    text = "Історія порожня. Якщо є інтернет — зачекайте або натисніть оновити.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schedules) { item ->
                        HistoryItem(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(item: OutageEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.dateText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = item.timesText,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatTime(item.publishedAt),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk", "UA"))
    return sdf.format(Date(millis))
}
