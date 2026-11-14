package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.HouseListing

data class PropertyVisualStyle(
  val key: String,
  val labelAm: String,
  val labelEn: String,
  @param:DrawableRes val drawableRes: Int,
)

val PropertyVisualStyles =
  listOf(
    PropertyVisualStyle(
      key = HouseListing.IMAGE_APARTMENT,
      labelAm = "አፓርታማ",
      labelEn = "Apartment",
      drawableRes = R.drawable.img_bole_apartment,
    ),
    PropertyVisualStyle(
      key = HouseListing.IMAGE_CONDO,
      labelAm = "ኮንዶሚኒየም",
      labelEn = "Condominium",
      drawableRes = R.drawable.img_cmc_condo,
    ),
    PropertyVisualStyle(
      key = HouseListing.IMAGE_VILLA,
      labelAm = "ቪላ / G+1",
      labelEn = "Villa / G+1",
      drawableRes = R.drawable.img_ayat_villa,
    ),
    PropertyVisualStyle(
      key = HouseListing.IMAGE_SERVICE,
      labelAm = "ሰርቪስ ቤት",
      labelEn = "Service House",
      drawableRes = R.drawable.img_gerji_service,
    ),
  )

@DrawableRes
fun propertyImageResFor(imageKey: String): Int =
  when (imageKey) {
    HouseListing.IMAGE_CONDO -> R.drawable.img_cmc_condo
    HouseListing.IMAGE_VILLA -> R.drawable.img_ayat_villa
    HouseListing.IMAGE_SERVICE -> R.drawable.img_gerji_service
    else -> R.drawable.img_bole_apartment
  }

fun launchPhoneDialer(context: Context, phoneNumber: String) {
  val sanitized = phoneNumber.filter { it.isDigit() || it == '+' }
  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized"))
  context.startActivity(intent)
}
