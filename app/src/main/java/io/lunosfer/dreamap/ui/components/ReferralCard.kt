package io.lunosfer.dreamap.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.ReferralUiState
import io.lunosfer.dreamap.ui.viewmodel.ReferralViewModel

@Composable
fun ReferralCard(
    modifier: Modifier = Modifier,
    referralViewModel: ReferralViewModel = viewModel()
) {
    val uiState by referralViewModel.uiState.collectAsState()
    val claimMessage by referralViewModel.claimMessage.collectAsState()
    val claimError by referralViewModel.claimError.collectAsState()
    val isClaiming by referralViewModel.isClaiming.collectAsState()

    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }

    LaunchedEffect(claimMessage) {
        claimMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            inputCode = ""
            referralViewModel.clearMessages()
        }
    }

    LaunchedEffect(claimError) {
        claimError?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            referralViewModel.clearMessages()
        }
    }

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
            // Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = AstralGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.referral_title),
                    color = AstralGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SerifFontFamily
                )
            }

            when (val s = uiState) {
                is ReferralUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp),
                        color = AstralGold
                    )
                }
                is ReferralUiState.Success -> {
                    val stats = s.stats
                    val code = stats.displayCode

                    // Referral Code Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Void800)
                            .border(1.dp, AetherViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.referral_your_code), color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = code,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = SerifFontFamily
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Referral Code", code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.referral_copied), Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.referral_copy), tint = AstralGold)
                        }
                    }

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = stringResource(R.string.referral_stat_friends), value = "${stats.totalReferrals}")
                        StatItem(label = stringResource(R.string.referral_stat_mana), value = "✨ ${stats.totalManaEarned}")
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Claim Referral Code Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.referral_enter_code_label),
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it.uppercase() },
                                placeholder = { Text(stringResource(R.string.referral_placeholder), color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AstralGold,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = { referralViewModel.claimCode(inputCode) },
                                enabled = inputCode.isNotBlank() && !isClaiming,
                                colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isClaiming) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                } else {
                                    Text(stringResource(R.string.referral_use_btn), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                is ReferralUiState.Error -> {
                    Text(text = s.message, color = Color(0xFFF87171), fontSize = 12.sp)
                }
                is ReferralUiState.Idle -> {}
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = AstralGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
    }
}
