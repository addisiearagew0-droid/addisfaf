package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HouseListing
import com.example.ui.components.PropertyVisualStyles
import com.example.ui.viewmodel.PostHouseUiState

@Composable
fun PostHouseScreen(
  uiState: PostHouseUiState,
  onSubmitHouse: (
    type: String,
    title: String,
    location: String,
    priceInput: String,
    bedrooms: Int,
    areaSqmInput: String,
    description: String,
    imageKey: String,
    phone: String,
    ownerName: String,
  ) -> Unit,
  onPostSuccessConsumed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedType by rememberSaveable { mutableStateOf(HouseListing.TYPE_RENT) }
  var selectedStyle by rememberSaveable { mutableStateOf(HouseListing.IMAGE_APARTMENT) }
  var title by rememberSaveable { mutableStateOf("") }
  var location by rememberSaveable { mutableStateOf("") }
  var priceInput by rememberSaveable { mutableStateOf("") }
  var bedrooms by rememberSaveable { mutableIntStateOf(2) }
  var areaSqmInput by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable { mutableStateOf("") }
  var phone by rememberSaveable { mutableStateOf("") }
  var ownerName by rememberSaveable { mutableStateOf("") }

  LaunchedEffect(uiState.successHouseId) {
    if (uiState.successHouseId != null) {
      title = ""
      location = ""
      priceInput = ""
      bedrooms = 2
      areaSqmInput = ""
      description = ""
      phone = ""
      ownerName = ""
      onPostSuccessConsumed()
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 680.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.AddHome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.post_header),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          // Type Segmented Selector
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "የአገልግሎት ዓይነት",
              style = MaterialTheme.typography.labelLarge,
            )
            val types = listOf(HouseListing.TYPE_RENT, HouseListing.TYPE_SALE)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              types.forEachIndexed { index, type ->
                SegmentedButton(
                  selected = selectedType == type,
                  onClick = { selectedType = type },
                  shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                  modifier = Modifier.testTag("post_type_$type"),
                ) {
                  Text(text = type, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }

          // Visual Style Picker
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "የቤቱ ዓይነት (ምስል ይምረጡ)",
              style = MaterialTheme.typography.labelLarge,
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              PropertyVisualStyles.forEach { style ->
                val isSelected = selectedStyle == style.key
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  border =
                    BorderStroke(
                      width = if (isSelected) 2.dp else 1.dp,
                      color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    ),
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surface,
                  modifier =
                    Modifier.width(122.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { selectedStyle = style.key }
                      .testTag("style_option_${style.key}"),
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Box {
                      Image(
                        painter = painterResource(id = style.drawableRes),
                        contentDescription = style.labelAm,
                        contentScale = ContentScale.Crop,
                        modifier =
                          Modifier.fillMaxWidth()
                            .height(66.dp)
                            .clip(RoundedCornerShape(10.dp)),
                      )
                      if (isSelected) {
                        Icon(
                          imageVector = Icons.Filled.CheckCircle,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = style.labelAm,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          }

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("የቤቱ መግለጫ (ለምሳሌ: ባለ 2 መኝታ አፓርታማ)") },
            placeholder = { Text("ባለ 2 መኝታ ኮንዶሚኒየም") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("post_title_input"),
          )

          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("ሰፈር / አድራሻ") },
            placeholder = { Text("ለምሳሌ: አያት 49") },
            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("post_location_input"),
          )

          OutlinedTextField(
            value = priceInput,
            onValueChange = { input -> priceInput = input.filter { it.isDigit() }.take(10) },
            label = {
              Text(if (selectedType == HouseListing.TYPE_RENT) "ወርሃዊ ዋጋ (በብር)" else "የሽያጭ ዋጋ (በብር)")
            },
            placeholder = {
              Text(if (selectedType == HouseListing.TYPE_RENT) "18000" else "18500000")
            },
            leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null) },
            suffix = {
              Text(if (selectedType == HouseListing.TYPE_RENT) "ብር / ወር" else "ብር")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("post_price_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Bedrooms Stepper
            Surface(
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.weight(1f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "መኝታ: $bedrooms",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Row {
                  IconButton(
                    onClick = { if (bedrooms > 0) bedrooms-- },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                  }
                  IconButton(
                    onClick = { if (bedrooms < 15) bedrooms++ },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                  }
                }
              }
            }

            OutlinedTextField(
              value = areaSqmInput,
              onValueChange = { input -> areaSqmInput = input.filter { it.isDigit() }.take(5) },
              label = { Text("ስፋት (ካሬ)") },
              placeholder = { Text("95") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("post_area_input"),
            )
          }

          OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(500) },
            label = { Text("ተጨማሪ መግለጫ (አማራጭ)") },
            placeholder = { Text("ውሃ፣ መብራት፣ ፓርኪንግ ወዘተ...") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("post_description_input"),
          )

          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.take(20) },
            label = { Text("የባለቤቱ ስልክ ቁጥር") },
            placeholder = { Text("0911...") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("post_phone_input"),
          )

          OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it.take(60) },
            label = { Text("የባለቤቱ ስም (አማራጭ)") },
            placeholder = { Text("ለምሳሌ: አቶ አበበ") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("post_owner_name_input"),
          )

          if (uiState.errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Button(
            onClick = {
              onSubmitHouse(
                selectedType,
                title,
                location,
                priceInput,
                bedrooms,
                areaSqmInput,
                description,
                selectedStyle,
                phone,
                ownerName,
              )
            },
            enabled = !uiState.isSubmitting,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(54.dp)
                .testTag("submit_post_button"),
          ) {
            if (uiState.isSubmitting) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("በመለጠፍ ላይ...")
            } else {
              Text(
                text = stringResource(R.string.post_submit),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }
}
