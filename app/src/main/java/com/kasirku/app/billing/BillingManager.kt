package com.kasirku.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Skeleton Play Billing untuk langganan KasirKu.
 *
 * TODO sebelum rilis:
 *  1. Buat produk langganan di Play Console → Monetize → Subscriptions,
 *     mis. productId "kasirku_premium_bulanan".
 *  2. Ganti PRODUCT_IDS di bawah dengan ID asli.
 *  3. Verifikasi purchase di backend (Realtime Developer Notifications /
 *     Purchases API) lalu tulis field `langganan` di dokumen toko Firestore
 *     lewat Admin SDK — JANGAN percaya status lokal saja untuk hal penting.
 */
class BillingManager(context: Context) {

    companion object {
        // TODO: ganti dengan product ID dari Play Console
        val PRODUCT_IDS = listOf("kasirku_premium_bulanan")
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { _, _ -> refresh() }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    fun connect(onReady: () -> Unit = {}) {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    refresh()
                    onReady()
                }
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(PRODUCT_IDS.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            })
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = details.productDetailsList
            }
        }
    }

    fun launchPurchase(activity: Activity, product: ProductDetails) {
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    fun refresh() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _isPremium.value = purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged
                }
            }
        }
    }

    fun disconnect() = client.endConnection()
}
