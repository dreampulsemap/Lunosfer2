package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.DailySeedItem
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.ui.components.AISummariesCard
import io.lunosfer.dreamap.ui.components.VisionGridCard
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.CompassUiState
import io.lunosfer.dreamap.ui.viewmodel.UiState
import io.lunosfer.dreamap.ui.viewmodel.VisionViewModel

@Composable
fun VisionScreen(
    onGoalClick: (String) -> Unit = {},
    viewModel: VisionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val compassState by viewModel.compassState.collectAsState()
    val dailySeeds by viewModel.dailySeeds.collectAsState()
    val seedGeneratingMap by viewModel.seedGeneratingMap.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Void950)) {
        when (val current = state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AstralGold)
            }
            is UiState.Error -> VisionError(message = current.message, onRetry = viewModel::retry)
            is UiState.Success -> VisionContent(
                goals = current.data,
                compassState = compassState,
                dailySeeds = dailySeeds,
                seedGeneratingMap = seedGeneratingMap,
                onFetchCompass = viewModel::fetchDailyCompass,
                onGenerateSeed = viewModel::generateSeedForGoal,
                onToggleSeed = viewModel::toggleSeedCompletion,
                onGoalClick = onGoalClick
            )
        }
    }
}

@Composable
private fun VisionError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.vision_load_failed), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily))
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.vision_retry))
        }
    }
}

@Composable
private fun VisionContent(
    goals: List<Goal>,
    compassState: CompassUiState,
    dailySeeds: List<DailySeedItem>,
    seedGeneratingMap: Map<String, Boolean>,
    onFetchCompass: () -> Unit,
    onGenerateSeed: (String) -> Unit,
    onToggleSeed: (DailySeedItem) -> Unit,
    onGoalClick: (String) -> Unit
) {
    val activeGoals = remember(goals) { goals.filter { it.status == "active" || it.status == null } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1) Günlük Pusula Kartı
        item {
            DailyCompassCard(compassState = compassState, onFetchCompass = onFetchCompass)
        }

        // 2) AI Özetleri Kartı (Haftalık / Aylık)
        item {
            AISummariesCard()
        }

        // 2) Günlük Tohumlar ("Bugün Yapman Gerekenler")
        item {
            DailySeedsSection(
                activeGoals = activeGoals,
                dailySeeds = dailySeeds,
                seedGeneratingMap = seedGeneratingMap,
                onGenerateSeed = onGenerateSeed,
                onToggleSeed = onToggleSeed,
                onGoalClick = onGoalClick
            )
        }

        // 3) Herkese Açık Vizyonlar Bölümü
        item {
            Text(
                text = stringResource(R.string.vision_public_visions_title),
                color = AstralGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (goals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.vision_no_public_visions_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.vision_no_public_visions_desc),
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Group goals into pairs for a 2-column grid inside LazyColumn
            val rows = goals.chunked(2)
            items(rows) { rowGoals ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (goal in rowGoals) {
                        Box(modifier = Modifier.weight(1f)) {
                            VisionGridCard(goal = goal, onClick = { onGoalClick(goal.id) })
                        }
                    }
                    if (rowGoals.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCompassCard(
    compassState: CompassUiState,
    onFetchCompass: () -> Unit
) {
    val parseColor: (String?) -> Color = { hex ->
        if (!hex.isNullOrBlank()) {
            try {
                val clean = hex.removePrefix("#")
                Color(android.graphics.Color.parseColor("#$clean"))
            } catch (_: Exception) {
                AstralGold
            }
        } else {
            AstralGold
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(
            1.dp,
            if (compassState is CompassUiState.Success) parseColor(compassState.color).copy(alpha = 0.6f)
            else AetherViolet.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧭", fontSize = 20.sp)
                    Text(
                        text = stringResource(R.string.vision_daily_compass_title),
                        color = AstralGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SerifFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AetherViolet.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.vision_jung_guide),
                        color = AstralGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            when (compassState) {
                is CompassUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.vision_compass_idle_text),
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onFetchCompass,
                        colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.vision_compass_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                is CompassUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AstralGold, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.vision_compass_loading), color = Color.LightGray, fontSize = 13.sp)
                    }
                }
                is CompassUiState.Success -> {
                    val accentColor = parseColor(compassState.color)
                    if (!compassState.archetype.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accentColor.copy(alpha = 0.2f))
                                .border(BorderStroke(0.5.dp, accentColor), shape = RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.vision_compass_archetype_label, compassState.archetype),
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = compassState.reading,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                is CompassUiState.AlreadyUsedToday -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Void800.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AstralGold, modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.vision_compass_already_used),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                is CompassUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(compassState.message, color = Color(0xFFF87171), fontSize = 12.sp)
                        Button(
                            onClick = onFetchCompass,
                            colors = ButtonDefaults.buttonColors(containerColor = Void800),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.vision_retry), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySeedsSection(
    activeGoals: List<Goal>,
    dailySeeds: List<DailySeedItem>,
    seedGeneratingMap: Map<String, Boolean>,
    onGenerateSeed: (String) -> Unit,
    onToggleSeed: (DailySeedItem) -> Unit,
    onGoalClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🌱", fontSize = 18.sp)
                Text(
                    text = stringResource(R.string.vision_daily_seeds_title),
                    color = AstralGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SerifFontFamily
                )
            }

            if (activeGoals.isEmpty()) {
                Text(
                    text = stringResource(R.string.vision_daily_seeds_no_active_goals),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            } else {
                for (goal in activeGoals) {
                    val existingSeed = dailySeeds.find { it.goalId == goal.id }
                    val isGenerating = seedGeneratingMap[goal.id] == true

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Void800.copy(alpha = 0.6f)),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = goal.title,
                                color = AstralGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onGoalClick(goal.id) }
                            )

                            if (existingSeed != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleSeed(existingSeed) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (existingSeed.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (existingSeed.isCompleted) AstralGold else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = existingSeed.seedText,
                                        color = if (existingSeed.isCompleted) Color.Gray else Color.White,
                                        fontSize = 13.sp,
                                        textDecoration = if (existingSeed.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.vision_daily_seeds_no_seed_text),
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { onGenerateSeed(goal.id) },
                                        enabled = !isGenerating,
                                        colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isGenerating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = AstralGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(R.string.vision_daily_seeds_generate_btn), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
