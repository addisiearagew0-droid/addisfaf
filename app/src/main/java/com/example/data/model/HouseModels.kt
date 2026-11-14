package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class HouseListing(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val type: String = TYPE_RENT,
  val title: String = "",
  val location: String = "",
  val priceDisplay: String = "",
  val priceAmount: Long = 0L,
  val bedrooms: Long = 1L,
  val areaSqm: Long = 0L,
  val description: String = "",
  val imageKey: String = IMAGE_APARTMENT,
  val status: String = STATUS_ACTIVE,
  val starterListing: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    const val TYPE_RENT = "ኪራይ"
    const val TYPE_SALE = "ሽያጭ"
    const val STATUS_ACTIVE = "active"
    const val STATUS_CLOSED = "closed"

    const val IMAGE_APARTMENT = "apartment"
    const val IMAGE_CONDO = "condo"
    const val IMAGE_VILLA = "villa"
    const val IMAGE_SERVICE = "service"
  }
}

@IgnoreExtraProperties
data class HouseContact(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val phone: String = "",
  val ownerName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ContactUnlock(
  @DocumentId val id: String = "",
  val userId: String = "",
  val houseId: String = "",
  val amountBirr: Long = 50L,
  val paymentMethod: String = "telebirr",
  val transactionRef: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
