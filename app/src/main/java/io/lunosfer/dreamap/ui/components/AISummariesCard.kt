package io.lunosfer.dreamap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.SummaryData
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.SummaryUiState
import io.lunosfer.dreamap.ui.viewmodel.SummaryViewModel

@Composable
fun AISummariesCard(
    modifier: Modifier = Modifier,
    summaryViewModel: SummaryViewModel = viewModel()
) {
    val weeklyState by summaryViewModel.weeklySummaryState.collectAsState()
    val monthlyState by summaryViewModel.monthlySummaryState.collectAsState()
    val isGenerating by summaryViewModel.isGenerating.collectAsState()

    var selectedPeriod by remember { mutableStateOf("weekly") } // "weekly" or "monthly"

    val currentState = if (selectedPeriod == "weekly") weeklyState else monthlyState

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📜", fontSize = 20.sp)
                    Text(
                        text = stringResource(R.string.summary_title),
                        color = AstralGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SerifFontFamily
                    )
                }

                // Toggle Weekly / Monthly
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Void800)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedPeriod == "weekly") AetherViolet else Color.Transparent)
                            .clickable { selectedPeriod = "weekly" }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.summary_weekly),
                            color = if (selectedPeriod == "weekly") Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedPeriod == "monthly") AetherViolet else Color.Transparent)
                            .clickable { selectedPeriod = "monthly" }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.summary_monthly),
                            color = if (selectedPeriod == "monthly") Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Body depending on state
            when (currentState) {
                is SummaryUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AstralGold,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isGenerating) stringResource(R.string.summary_generating_ai) else stringResource(R.string.summary_loading),
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                }
                is SummaryUiState.Success -> {
                    val summary = currentState.summary
                    if (summary != null && summary.text.isNotBlank()) {
                        SummaryContentView(
                            summary = summary,
                            periodType = selectedPeriod,
                            isGenerating = isGenerating,
                            onGenerateNew = { summaryViewModel.generateSummary(selectedPeriod) }
                        )
                    } else {
                        EmptySummaryView(
                            periodType = selectedPeriod,
                            isGenerating = isGenerating,
                            onGenerate = { summaryViewModel.generateSummary(selectedPeriod) }
                        )
                    }
                }
                is SummaryUiState.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentState.message,
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { summaryViewModel.generateSummary(selectedPeriod) },
                            colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.summary_create_btn), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                is SummaryUiState.Idle -> {
                    EmptySummaryView(
                        periodType = selectedPeriod,
                        isGenerating = isGenerating,
                        onGenerate = { summaryViewModel.generateSummary(selectedPeriod) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContentView(
    summary: SummaryData,
    periodType: String,
    isGenerating: Boolean,
    onGenerateNew: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Badges row (Dream count & Sentiment)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (summary.dreamCount != null && summary.dreamCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AetherViolet.copy(alpha = 0.25f))
                        .border(0.5.dp, AstralGold.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.summary_dreams_analyzed, summary.dreamCount),
                        color = AstralGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!summary.dominantSentiment.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Void800)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.summary_sentiment, summary.dominantSentiment),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Dominant Archetypes Chips
        if (!summary.dominantArchetypes.isNullOrEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                summary.dominantArchetypes.forEach { arch ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AetherViolet.copy(alpha = 0.3f))
                            .border(0.5.dp, AstralGold.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = arch,
                            color = AstralGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Summary Text
        Text(
            text = summary.text,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        // Regenerate Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onGenerateNew,
                enabled = !isGenerating
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = AstralGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.summary_refresh),
                    color = AstralGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptySummaryView(
    periodType: String,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    val periodLabel = if (periodType == "weekly") stringResource(R.string.summary_weekly) else stringResource(R.string.summary_monthly)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.summary_not_created, periodLabel),
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.summary_synthesizing), color = Color.White, fontSize = 13.sp)
            } else {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AstralGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.summary_create_period_btn, periodLabel),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
