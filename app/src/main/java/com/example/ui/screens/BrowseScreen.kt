package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ContactUnlock
import com.example.data.model.HouseContact
import com.example.data.model.HouseListing
import com.example.ui.components.launchPhoneDialer
import com.example.ui.components.propertyImageResFor
import com.example.ui.theme.AmberBadge
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.UnlockedGreenBg
import com.example.ui.theme.UnlockedGreenBorder
import com.example.ui.theme.UnlockedGreenText
import com.example.ui.viewmodel.HouseViewModel
import com.example.ui.viewmodel.UiState

private val QUICK_NEIGHBORHOODS = listOf("ቦሌ", "ሲኤምሲ", "አያት", "ገርጂ")

@Composable
fun BrowseScreen(
  housesState: UiState<List<HouseListing>>,
  searchQuery: String,
  selectedTypeFilter: String,
  myUnlocks: Map<String, ContactUnlock>,
  revealedContacts: Map<String, HouseContact>,
  currentUserId: String,
  onSearchQueryChanged: (String) -> Unit,
  onTypeFilterSelected: (String) -> Unit,
  onUnlockClicked: (HouseListing) -> Unit,
  onHouseClicked: (HouseListing) -> Unit,
  onEnsureContactLoaded: (HouseListing) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Search & Filters Card
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 720.dp)
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChanged,
          singleLine = true,
          placeholder = {
            Text(
              text = stringResource(R.string.search_placeholder),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.primary,
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChanged("") },
                modifier = Modifier.testTag("clear_search_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Clear,
                  contentDescription = "Clear search",
                )
              }
            }
          },
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              unfocusedContainerColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.fillMaxWidth().testTag("search_input"),
        )

        // Type Filter Row + Quick Neighborhood Chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val filters =
            listOf(
              HouseViewModel.FILTER_ALL to stringResource(R.string.filter_all),
              HouseListing.TYPE_RENT to stringResource(R.string.filter_rent),
              HouseListing.TYPE_SALE to stringResource(R.string.filter_sale),
            )
          filters.forEach { (key, label) ->
            val selected = selectedTypeFilter == key
            FilterChip(
              selected = selected,
              onClick = { onTypeFilterSelected(key) },
              label = { Text(text = label, fontWeight = FontWeight.SemiBold) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.testTag("filter_chip_$key"),
            )
          }

          Box(
            modifier =
              Modifier.width(1.dp)
                .height(22.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
          )

          QUICK_NEIGHBORHOODS.forEach { area ->
            val areaSelected = searchQuery.equals(area, ignoreCase = true)
            FilterChip(
              selected = areaSelected,
              onClick = {
                onSearchQueryChanged(if (areaSelected) "" else area)
              },
              label = { Text(area) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              modifier = Modifier.testTag("area_chip_$area"),
            )
          }
        }
      }
    }

    when (housesState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "ቤቶችን በመጫን ላይ...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Text(
              text = housesState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.padding(18.dp),
            )
          }
        }
      }
      is UiState.Success -> {
        val houses = housesState.data
        if (houses.isEmpty()) {
          EmptyBrowseState(
            hasFilter = searchQuery.isNotBlank() || selectedTypeFilter != HouseViewModel.FILTER_ALL,
            onClearFilters = {
              onSearchQueryChanged("")
              onTypeFilterSelected(HouseViewModel.FILTER_ALL)
            },
          )
        } else {
          LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
          ) {
            item(key = "commission_banner") {
              PromoHighlightBanner(totalCount = houses.size)
            }
            items(items = houses, key = { it.id }) { house ->
              val isUnlocked =
                myUnlocks.containsKey(house.id) ||
                  (!house.starterListing && house.ownerId == currentUserId)
              val contact = revealedContacts[house.id]
              LaunchedEffect(house.id, isUnlocked) {
                if (isUnlocked) {
                  onEnsureContactLoaded(house)
                }
              }
              HouseListingCard(
                house = house,
                isUnlocked = isUnlocked,
                isOwnedByCurrentUser = !house.starterListing && house.ownerId == currentUserId,
                revealedPhone = contact?.phone,
                onCardClick = { onHouseClicked(house) },
                onUnlockClick = { onUnlockClicked(house) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PromoHighlightBanner(totalCount: Int) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier.background(
            Brush.horizontalGradient(listOf(EmeraldDark, EmeraldPrimary))
          )
          .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier.size(38.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.HomeWork,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "ያለ ደላላ ቀጥታ ግንኙነት",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "50 ብር በቴሌብር ብቻ • 0% ኮሚሽን",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.88f),
          )
        }
      }
      Surface(
        color = AmberBadge,
        shape = RoundedCornerShape(20.dp),
      ) {
        Text(
          text = "$totalCount ቤቶች",
          color = Color.White,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }
  }
}

@Composable
private fun HouseListingCard(
  house: HouseListing,
  isUnlocked: Boolean,
  isOwnedByCurrentUser: Boolean,
  revealedPhone: String?,
  onCardClick: () -> Unit,
  onUnlockClick: () -> Unit,
) {
  val context = LocalContext.current

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onCardClick)
        .testTag("house_card_${house.id}"),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(186.dp)) {
        Image(
          painter = painterResource(id = propertyImageResFor(house.imageKey)),
          contentDescription = house.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize(),
        )
        Box(
          modifier =
            Modifier.matchParentSize()
              .background(
                Brush.verticalGradient(
                  colors =
                    listOf(
                      Color.Black.copy(alpha = 0.15f),
                      Color.Transparent,
                      Color.Black.copy(alpha = 0.52f),
                    )
                )
              )
        )

        // Top badges row
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color =
              if (house.type == HouseListing.TYPE_SALE) EmeraldPrimary else AmberBadge,
            shape = RoundedCornerShape(20.dp),
          ) {
            Text(
              text = house.type,
              color = Color.White,
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
          }

          AnimatedVisibility(visible = isOwnedByCurrentUser || isUnlocked) {
            Surface(
              color = UnlockedGreenBg,
              shape = RoundedCornerShape(20.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Verified,
                  contentDescription = null,
                  tint = UnlockedGreenText,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isOwnedByCurrentUser) "የእርስዎ ቤት" else "የተከፈተ",
                  color = UnlockedGreenText,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        // Bottom metadata pills inside photo
        Row(
          modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (house.bedrooms > 0) {
            PhotoMetaPill(icon = Icons.Filled.Bed, text = "${house.bedrooms} መኝታ")
          }
          if (house.areaSqm > 0) {
            PhotoMetaPill(icon = Icons.Filled.AspectRatio, text = "${house.areaSqm} ካሬ")
          }
        }
      }

      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = house.priceDisplay,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = house.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = house.location,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isUnlocked) {
          val phoneText = revealedPhone ?: "በመጫን ላይ..."
          OutlinedButton(
            onClick = {
              if (!revealedPhone.isNullOrBlank()) {
                launchPhoneDialer(context, revealedPhone)
              }
            },
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = UnlockedGreenBg,
                contentColor = UnlockedGreenText,
              ),
            border = BorderStroke(1.dp, UnlockedGreenBorder),
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .testTag("call_unlocked_button_${house.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.Call,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                if (revealedPhone != null) "የባለቤቱ ስልክ: $phoneText"
                else "ስልክ በመጫን ላይ...",
              fontWeight = FontWeight.Bold,
            )
          }
        } else {
          Button(
            onClick = onUnlockClick,
            shape = RoundedCornerShape(12.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .testTag("unlock_phone_button_${house.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.LockOpen,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.unlock_cta),
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PhotoMetaPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
) {
  Surface(
    color = Color.Black.copy(alpha = 0.56f),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}

@Composable
private fun EmptyBrowseState(
  hasFilter: Boolean,
  onClearFilters: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(28.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Filled.SearchOff,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        modifier = Modifier.size(56.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "ምንም ቤት አልተገኘም።",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          if (hasFilter) "የፍለጋ ቃሉን ወይም የቤት ዓይነቱን ቀይረው ይሞክሩ።"
          else "እስካሁን የተለጠፈ ቤት የለም። የመጀመሪያውን ቤት ይለጥፉ!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (hasFilter) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(onClick = onClearFilters) {
          Text("ፍለጋውን አጽዳ")
        }
      }
    }
  }
}
