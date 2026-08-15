package io.lunosfer.dreamap.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.repository.AuraPackOffer
import io.lunosfer.dreamap.data.repository.BillingRepository
import io.lunosfer.dreamap.data.repository.PremiumPlanOffer
import io.lunosfer.dreamap.data.repository.PurchaseFlowState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * BillingRepository uygulama ömrü boyunca tek bir singleton olduğu için bu
 * ViewModel esasen ince bir UI katmanı: repository'nin StateFlow'larını
 * sheet açıkken canlı tutar (WhileSubscribed) ve satın alma tetikleyicilerini
 * Activity referansıyla birlikte repository'ye iletir.
 */
class BillingViewModel(
    private val repository: BillingRepository = BillingRepository
) : ViewModel() {

    val auraOffers: StateFlow<List<AuraPackOffer>> = repository.auraOffers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val premiumOffers: StateFlow<List<PremiumPlanOffer>> = repository.premiumOffers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseState: StateFlow<PurchaseFlowState> = repository.purchaseState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PurchaseFlowState.Idle)

    val auraBalance: StateFlow<Int> = repository.auraBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        repository.connectAndLoadProducts()
    }

    fun buyAura(activity: Activity, productId: String) {
        viewModelScope.launch { repository.launchAuraPurchase(activity, productId) }
    }

    fun buyPremium(activity: Activity, basePlanId: String) {
        viewModelScope.launch { repository.launchPremiumPurchase(activity, basePlanId) }
    }

    fun dismissStatus() {
        repository.resetPurchaseState()
    }
}
