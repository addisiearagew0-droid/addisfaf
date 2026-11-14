package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.HouseListing
import com.example.data.repository.HouseRepository
import com.example.ui.components.HouseDetailSheet
import com.example.ui.components.TelebirrPaymentDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BrowseScreen
import com.example.ui.screens.MyAccountScreen
import com.example.ui.screens.PostHouseScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.HouseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class YaledelalaTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  BROWSE(R.string.tab_browse, Icons.Filled.Search, Icons.Outlined.Search, "nav_browse"),
  POST(R.string.tab_post, Icons.Filled.AddHome, Icons.Outlined.AddHome, "nav_post"),
  ACCOUNT(R.string.tab_my_account, Icons.Filled.Person, Icons.Outlined.Person, "nav_account"),
}

@Composable
fun YaledelalaApp(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      MainFeatureScreen(user = user)
    }
  }
}

@Composable
private fun MainFeatureScreen(user: FirebaseUser) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val houseViewModel: HouseViewModel =
    viewModel(
      key = user.uid,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            HouseViewModel(
              repository = HouseRepository(db),
              currentUserId = user.uid,
            )
          }
        },
    )

  var currentTab by rememberSaveable { mutableStateOf(YaledelalaTab.BROWSE) }
  var selectedDetailHouse by remember { mutableStateOf<HouseListing?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  val filteredHousesState by houseViewModel.filteredHousesState.collectAsStateWithLifecycle()
  val activeHousesState by houseViewModel.activeHousesState.collectAsStateWithLifecycle()
  val myHousesState by houseViewModel.myHousesState.collectAsStateWithLifecycle()
  val searchQuery by houseViewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedTypeFilter by houseViewModel.selectedTypeFilter.collectAsStateWithLifecycle()
  val myUnlocks by houseViewModel.myUnlocks.collectAsStateWithLifecycle()
  val revealedContacts by houseViewModel.revealedContacts.collectAsStateWithLifecycle()
  val unlockModalState by houseViewModel.unlockModalState.collectAsStateWithLifecycle()
  val postHouseUiState by houseViewModel.postHouseUiState.collectAsStateWithLifecycle()
  val statusMessage by houseViewModel.statusMessage.collectAsStateWithLifecycle()

  LaunchedEffect(statusMessage) {
    val msg = statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      houseViewModel.clearStatusMessage()
    }
  }

  if (currentTab != YaledelalaTab.BROWSE) {
    BackHandler { currentTab = YaledelalaTab.BROWSE }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = { YaledelalaTopBanner() },
      bottomBar = {
        if (!useRail) {
          NavigationBar {
            YaledelalaTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail {
            YaledelalaTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (currentTab) {
            YaledelalaTab.BROWSE -> {
              BrowseScreen(
                housesState = filteredHousesState,
                searchQuery = searchQuery,
                selectedTypeFilter = selectedTypeFilter,
                myUnlocks = myUnlocks,
                revealedContacts = revealedContacts,
                currentUserId = user.uid,
                onSearchQueryChanged = houseViewModel::onSearchQueryChanged,
                onTypeFilterSelected = houseViewModel::onTypeFilterSelected,
                onUnlockClicked = houseViewModel::openUnlockModal,
                onHouseClicked = { house ->
                  houseViewModel.ensureContactLoaded(house)
                  selectedDetailHouse = house
                },
                onEnsureContactLoaded = houseViewModel::ensureContactLoaded,
              )
            }
            YaledelalaTab.POST -> {
              PostHouseScreen(
                uiState = postHouseUiState,
                onSubmitHouse = houseViewModel::createHouseListing,
                onPostSuccessConsumed = {
                  houseViewModel.clearPostSuccess()
                  currentTab = YaledelalaTab.BROWSE
                },
              )
            }
            YaledelalaTab.ACCOUNT -> {
              MyAccountScreen(
                userDisplayName = user.displayName,
                userEmail = user.email,
                myHousesState = myHousesState,
                activeHousesState = activeHousesState,
                myUnlocks = myUnlocks,
                revealedContacts = revealedContacts,
                onMarkClosed = houseViewModel::markListingClosed,
                onDeleteHouse = houseViewModel::deleteListing,
                onSignOut = {
                  signOutUser(
                    credentialManager = credentialManager,
                    scope = coroutineScope,
                  )
                },
              )
            }
          }
        }
      }
    }
  }

  val activeDetail = selectedDetailHouse
  if (activeDetail != null) {
    val isUnlocked =
      myUnlocks.containsKey(activeDetail.id) ||
        (!activeDetail.starterListing && activeDetail.ownerId == user.uid)
    HouseDetailSheet(
      house = activeDetail,
      revealedContact = revealedContacts[activeDetail.id],
      isUnlockedOrOwned = isUnlocked,
      onUnlockClick = { houseViewModel.openUnlockModal(activeDetail) },
      onDismiss = { selectedDetailHouse = null },
    )
  }

  val modalState = unlockModalState
  if (modalState != null) {
    TelebirrPaymentDialog(
      state = modalState,
      onConfirmPayment = houseViewModel::confirmTelebirrPayment,
      onDismiss = houseViewModel::closeUnlockModal,
    )
  }
}

@Composable
private fun YaledelalaTopBanner() {
  Surface(shadowElevation = 4.dp) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.horizontalGradient(listOf(EmeraldDark, EmeraldPrimary)))
          .statusBarsPadding()
          .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
      Text(
        text = "🏠 ${stringResource(R.string.app_name)} (Yaledelala)",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.app_subtitle),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.9f),
      )
    }
  }
}
