package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

/**
 * Play Console'da oluşturulan ürün kimlikleriyle BİREBİR aynı olmalı.
 * Web tarafındaki eşleniği: dreamap-frontend/lib/googlePlayProducts.js —
 * ikisi de tek doğruluk kaynağının iki farklı dildeki kopyası, biri
 * değişirse diğeri de güncellenmeli.
 */
object BillingProductIds {
    const val PREMIUM_SUBSCRIPTION = "premium_membership"

    // basePlanId'ler — Play Console'da bu abonelik ürünü altında bu isimlerle
    // temel plan (base plan) oluşturulmalı.
    const val PLAN_MONTHLY = "monthly"
    const val PLAN_QUARTERLY = "quarterly"
    const val PLAN_YEARLY = "yearly"

    // Aura paketleri — her biri Play Console'da ayrı bir tek seferlik
    // (tüketilebilir) ürün. Aura miktarı asla client'ta hesaplanmaz;
    // backend, productId'ye bakarak kendi map'inden okur (bkz.
    // googlePlayProducts.js) — client bu listeyi yalnızca hangi ürünleri
    // sorgulayacağını bilmek için kullanır.
    val AURA_PACKS = listOf("aura_pack_10", "aura_pack_50", "aura_pack_120", "aura_pack_300")

    private val AURA_COUNTS = mapOf(
        "aura_pack_10" to 10,
        "aura_pack_50" to 50,
        "aura_pack_120" to 120,
        "aura_pack_300" to 300
    )

    // Yalnızca UI'da gösterim için (ör. "50 Aura"); gerçek bakiye ekleme
    // işlemi her zaman backend'de, aynı map'in JS kopyasından yapılır —
    // client'ın gönderdiği miktara güvenilmez.
    fun auraCountForProductId(productId: String): Int? = AURA_COUNTS[productId]
}

@Serializable
data class VerifyGooglePlayPurchaseRequest(
    val purchaseToken: String,
    val productId: String,
    val purchaseType: String // "subscription" | "aura_pack"
)

@Serializable
data class VerifyGooglePlayPurchaseResponse(
    val ok: Boolean = false,
    val status: String = "",
    val aurasAdded: Int = 0,
    val duplicate: Boolean = false
)
