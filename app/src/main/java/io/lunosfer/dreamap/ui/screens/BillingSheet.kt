package io.lunosfer.dreamap.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.repository.AuraPackOffer
import io.lunosfer.dreamap.data.repository.PremiumPlanOffer
import io.lunosfer.dreamap.data.repository.PurchaseFlowState
import io.lunosfer.dreamap.ui.theme.AstralAmber
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.SemanticDanger400
import io.lunosfer.dreamap.ui.theme.SemanticSuccess400
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.viewmodel.BillingViewModel

enum class BillingTab { AURA, PREMIUM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingSheet(
    initialTab: BillingTab,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    viewModel: BillingViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val auraOffers by viewModel.auraOffers.collectAsStateWithLifecycle()
    val premiumOffers by viewModel.premiumOffers.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(if (initialTab == BillingTab.AURA) 0 else 1) }

    // Satın alma başarıyla bittiğinde sheet'i otomatik kapatıyoruz —
    // kullanıcı sonucu ProfileScreen/TopBar'daki güncellenen bakiyeden görür.
    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseFlowState.Success) {
            kotlinx.coroutines.delay(900)
            viewModel.dismissStatus()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Void900
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Void900,
                contentColor = AstralGold
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.billing_aura_sheet_title)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.billing_premium_sheet_title)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            when (val state = purchaseState) {
                is PurchaseFlowState.Processing -> StatusBanner(
                    text = stringResource(R.string.billing_purchase_processing),
                    color = AstralGold
                )
                is PurchaseFlowState.Success -> StatusBanner(
                    text = if (state.status == "premium_activated") {
                        stringResource(R.string.billing_purchase_success_premium)
                    } else {
                        stringResource(R.string.billing_purchase_success_aura, state.aurasAdded)
                    },
                    color = SemanticSuccess400
                )
                is PurchaseFlowState.Error -> StatusBanner(
                    text = stringResource(R.string.billing_purchase_error),
                    color = SemanticDanger400
                )
                PurchaseFlowState.Idle -> Unit
            }

            if (selectedTab == 0) {
                AuraPackList(offers = auraOffers) { productId ->
                    activity?.let { viewModel.buyAura(it, productId) }
                }
            } else {
                PremiumPlanList(offers = premiumOffers) { basePlanId ->
                    activity?.let { viewModel.buyPremium(it, basePlanId) }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun AuraPackList(offers: List<AuraPackOffer>, onBuy: (String) -> Unit) {
    if (offers.isEmpty()) {
        LoadingRow()
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(offers, key = { it.productId }) { offer ->
            Surface(
                color = Void800,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = AstralGold)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.billing_aura_pack_label, offer.auraCount),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onBuy(offer.productId) },
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold, contentColor = Color.Black)
                    ) {
                        Text(offer.formattedPrice, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumPlanList(offers: List<PremiumPlanOffer>, onBuy: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AstralGold)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.billing_premium_sheet_subtitle),
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(14.dp))

        if (offers.isEmpty()) {
            LoadingRow()
            return
        }

        offers.forEach { offer ->
            Surface(
                color = Void800,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AstralAmber)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(planLabelRes(offer.basePlanId)),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onBuy(offer.basePlanId) },
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold, contentColor = Color.Black)
                    ) {
                        Text(offer.formattedPrice, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun planLabelRes(basePlanId: String) = when (basePlanId) {
    "monthly" -> R.string.billing_plan_monthly
    "quarterly" -> R.string.billing_plan_quarterly
    "yearly" -> R.string.billing_plan_yearly
    else -> R.string.billing_plan_monthly
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.billing_no_products), color = Color.Gray, fontSize = 13.sp)
        }
    }
}
