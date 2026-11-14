package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ContactUnlock
import com.example.data.model.HouseContact
import com.example.data.model.HouseListing
import com.example.ui.components.launchPhoneDialer
import com.example.ui.theme.UnlockedGreenBg
import com.example.ui.theme.UnlockedGreenText
import com.example.ui.viewmodel.UiState

@Composable
fun MyAccountScreen(
  userDisplayName: String?,
  userEmail: String?,
  myHousesState: UiState<List<HouseListing>>,
  activeHousesState: UiState<List<HouseListing>>,
  myUnlocks: Map<String, ContactUnlock>,
  revealedContacts: Map<String, HouseContact>,
  onMarkClosed: (String) -> Unit,
  onDeleteHouse: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val allActiveById =
    (activeHousesState as? UiState.Success)?.data?.associateBy { it.id } ?: emptyMap()
  val unlockedList = myUnlocks.values.toList()
  val myHouses = (myHousesState as? UiState.Success)?.data ?: emptyList()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
    ) {
      // User Profile Card
      item(key = "profile_card") {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier =
                  Modifier.size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.Person,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = userDisplayName?.takeIf { it.isNotBlank() } ?: "የያለ ደላላ ተጠቃሚ",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (!userEmail.isNullOrBlank()) {
                  Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
            OutlinedButton(
              onClick = onSignOut,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(stringResource(R.string.sign_out))
            }
          }
        }
      }

      // Section 1: Unlocked Owner Contacts
      item(key = "unlocked_header") {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.LockOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "የተከፈቱ ስልኮች (${unlockedList.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (unlockedList.isEmpty()) {
        item(key = "unlocked_empty") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "እስካሁን የከፈቱት የባለቤት ስልክ የለም። በቴሌብር 50 ብር በመክፈል ከባለቤቱ ጋር በቀጥታ ይገናኙ።",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(items = unlockedList, key = { "unlock_${it.houseId}" }) { unlock ->
          val house = allActiveById[unlock.houseId]
          val contact = revealedContacts[unlock.houseId]
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = house?.title ?: "የቤት ዝርዝር",
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (house != null) {
                  Text(
                    text = "${house.location} • ${house.priceDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = "Telebirr Ref: ${unlock.transactionRef}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              if (contact != null) {
                OutlinedButton(
                  onClick = { launchPhoneDialer(context, contact.phone) },
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      containerColor = UnlockedGreenBg,
                      contentColor = UnlockedGreenText,
                    ),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(contact.phone, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // Section 2: My Posted Listings
      item(key = "my_posts_header") {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "የእኔ የተለጠፉ ቤቶች (${myHouses.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (myHouses.isEmpty()) {
        item(key = "my_posts_empty") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "እስካሁን የለጠፉት ቤት የለም። የሚከራይ ወይም የሚሸጥ ቤት ካለዎት 'ቤት ለጥፍ' የሚለውን ይጫኑ።",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(items = myHouses, key = { "mine_${it.id}" }) { house ->
          val isClosed = house.status == HouseListing.STATUS_CLOSED
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = house.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "${house.location} • ${house.priceDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Surface(
                  color =
                    if (isClosed) MaterialTheme.colorScheme.surfaceVariant
                    else UnlockedGreenBg,
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text(
                    text = if (isClosed) "ተዘግቷል" else "ክፍት",
                    color =
                      if (isClosed) MaterialTheme.colorScheme.onSurfaceVariant
                      else UnlockedGreenText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
              ) {
                if (!isClosed) {
                  TextButton(onClick = { onMarkClosed(house.id) }) {
                    Icon(
                      imageVector = Icons.Filled.CheckCircle,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ተከራይቷል / ተሽጧል")
                  }
                }
                IconButton(onClick = { onDeleteHouse(house.id) }) {
                  Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete listing",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
