package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DiaryJournalUiState
import io.lunosfer.dreamap.ui.viewmodel.DiaryJournalViewModel
import io.lunosfer.dreamap.ui.viewmodel.UNKNOWN_DATE_KEY
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Profildeki KALICI Günce ekranı. Üstteki halka/hikaye şeridi bilinçli
// olarak Instagram diliyle konuşur (bkz. DiaryRingsBar) — hızlı, günlük,
// 24 saatte söner. Burası tam tersi: otomatik ilerleme yok, halka/avatar
// yok — tarihe göre gruplanmış, kendi hızında okunan kalıcı bir arşiv.

// ThreadScreen.kt'deki parseSupabaseTimestamp/dayLabel/timeLabel ile aynı
// desen (bu dosyaya özel — Kotlin'de private top-level fonksiyonlar dosyalar
// arası paylaşılamıyor).
private fun parseSupabaseTimestamp(isoTimestamp: String): java.util.Date? {
    return try {
        val withoutOffset = isoTimestamp
            .replace(Regex("[+-]\\d{2}:\\d{2}$"), "")
            .removeSuffix("Z")
        val truncated = if (withoutOffset.contains(".")) {
            val (base, fraction) = withoutOffset.split(".", limit = 2)
            base + "." + fraction.take(3).padEnd(3, '0')
        } else {
            "$withoutOffset.000"
        }
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        format.parse(truncated)
    } catch (e: Exception) {
        null
    }
}

private fun entryTimeLabel(isoTimestamp: String?): String {
    val date = isoTimestamp?.let { parseSupabaseTimestamp(it) } ?: return ""
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    return formatter.format(date)
}

// dateKey "yyyy-MM-dd" formatında (bkz. DiaryJournalViewModel) ya da
// UNKNOWN_DATE_KEY. Bugün/Dün ikisi de cihazın yerel saat dilimine göre
// hesaplanır.
private fun dayGroupLabel(dateKey: String, todayLabel: String, yesterdayLabel: String, unknownLabel: String): String {
    if (dateKey == UNKNOWN_DATE_KEY) return unknownLabel
    val parsed = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
    } catch (e: Exception) {
        null
    } ?: return dateKey

    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    val cal = Calendar.getInstance().apply { time = parsed }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        sameDay(cal, today) -> todayLabel
        sameDay(cal, yesterday) -> yesterdayLabel
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(parsed)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryJournalScreen(
    userId: String,
    onBack: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    val factory = remember(userId) { DiaryJournalViewModel.Factory(userId) }
    val viewModel: DiaryJournalViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    val todayLabel = stringResource(R.string.diary_journal_today)
    val yesterdayLabel = stringResource(R.string.diary_journal_yesterday)
    val unknownLabel = stringResource(R.string.diary_journal_unknown_date)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.diary_journal_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.diary_journal_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is DiaryJournalUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is DiaryJournalUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = SemanticDanger400)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadEntries() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.diary_journal_retry))
                        }
                    }
                }
                is DiaryJournalUiState.Success -> {
                    if (s.groupedEntries.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.diary_journal_empty_title),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.diary_journal_empty_body),
                                color = Color.Gray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            s.groupedEntries.forEach { (dateKey, entries) ->
                                item {
                                    Text(
                                        text = dayGroupLabel(dateKey, todayLabel, yesterdayLabel, unknownLabel),
                                        color = AstralGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }

                                items(entries, key = { it.id }) { entry ->
                                    JournalEntryCard(entry = entry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(entry: DiaryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Void800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Time header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entryTimeLabel(entry.createdAt),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Privacy indicator
                if (entry.visibility == "private") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Void800)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(stringResource(R.string.diary_journal_private_badge), color = AstralGold, fontSize = 10.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AetherCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(stringResource(R.string.diary_journal_public_badge), color = AetherCyan, fontSize = 10.sp)
                    }
                }
            }

            // Image / Video thumbnail (if available)
            val thumbUrl = entry.posterUrl ?: entry.mediaUrl
            if (entry.mediaType != "text" && !thumbUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Void950)
                )
            }

            // Caption
            if (!entry.caption.isNullOrBlank()) {
                Text(
                    text = entry.caption,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Goal Tag
            if (!entry.goalTitle.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.diary_journal_goal_prefix, entry.goalTitle),
                    color = AetherIndigo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
