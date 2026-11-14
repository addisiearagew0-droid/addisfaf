package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HouseContact
import com.example.data.model.HouseListing
import com.example.ui.theme.AmberBadge
import com.example.ui.theme.UnlockedGreenBg
import com.example.ui.theme.UnlockedGreenText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailSheet(
  house: HouseListing,
  revealedContact: HouseContact?,
  isUnlockedOrOwned: Boolean,
  onUnlockClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp)
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
      ) {
        Image(
          painter = painterResource(id = propertyImageResFor(house.imageKey)),
          contentDescription = house.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize(),
        )
        Surface(
          color =
            if (house.type == HouseListing.TYPE_SALE) MaterialTheme.colorScheme.primary
            else AmberBadge,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
        ) {
          Text(
            text = house.type,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = house.priceDisplay,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = house.title,
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = house.location,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (house.bedrooms > 0) {
          SpecChip(
            icon = Icons.Filled.Bed,
            label = "${house.bedrooms} መኝታ",
          )
        }
        if (house.areaSqm > 0) {
          SpecChip(
            icon = Icons.Filled.AspectRatio,
            label = "${house.areaSqm} ካሬ ሜትር",
          )
        }
      }

      if (house.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "የቤቱ ዝርዝር መግለጫ",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = house.description,
          style = MaterialTheme.typography.bodyLarge,
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      if (isUnlockedOrOwned && revealedContact != null) {
        if (revealedContact.ownerName.isNotBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ባለቤት: ${revealedContact.ownerName}",
              style = MaterialTheme.typography.titleMedium,
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
        }
        Button(
          onClick = { launchPhoneDialer(context, revealedContact.phone) },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = UnlockedGreenBg,
              contentColor = UnlockedGreenText,
            ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("detail_call_button"),
        ) {
          Icon(imageVector = Icons.Filled.Call, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "የባለቤቱ ስልክ: ${revealedContact.phone}",
            fontWeight = FontWeight.Bold,
          )
        }
      } else {
        Button(
          onClick = {
            onDismiss()
            onUnlockClick()
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("detail_unlock_button"),
        ) {
          Icon(imageVector = Icons.Filled.LockOpen, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("ስልክ ቁጥር አሳይ (50 ብር)")
        }
      }
    }
  }
}

@Composable
private fun SpecChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(10.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}
