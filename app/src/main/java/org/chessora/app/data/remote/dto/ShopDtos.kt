package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Specchio di Chessora.Contracts.Shop.ProductDto (GET /api/shop/products).
 * Sola consultazione in questa v1 (docs/android-app-spec.md §1): niente
 * carrello/acquisto in app, solo vetrina - vedi ui/shop/ShopScreen.kt.
 */
@Serializable
data class ShopProduct(
    val id: Int,
    val name: String,
    val description: String? = null,
    val price: Double,
    val imagePath: String? = null,
    val stockQuantity: Int? = null,
)
