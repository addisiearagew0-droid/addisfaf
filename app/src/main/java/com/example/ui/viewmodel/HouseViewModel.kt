package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactUnlock
import com.example.data.model.HouseContact
import com.example.data.model.HouseListing
import com.example.data.repository.HouseRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class UnlockModalState(
  val house: HouseListing,
  val isProcessing: Boolean = false,
  val unlockedContact: HouseContact? = null,
  val transactionRef: String? = null,
  val errorMessage: String? = null,
)

data class PostHouseUiState(
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
  val successHouseId: String? = null,
)

class HouseViewModel(
  private val repository: HouseRepository,
  val currentUserId: String,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedTypeFilter = MutableStateFlow(FILTER_ALL)
  val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

  private val _revealedContacts = MutableStateFlow<Map<String, HouseContact>>(emptyMap())
  val revealedContacts: StateFlow<Map<String, HouseContact>> = _revealedContacts.asStateFlow()

  private val _unlockModalState = MutableStateFlow<UnlockModalState?>(null)
  val unlockModalState: StateFlow<UnlockModalState?> = _unlockModalState.asStateFlow()

  private val _postHouseUiState = MutableStateFlow(PostHouseUiState())
  val postHouseUiState: StateFlow<PostHouseUiState> = _postHouseUiState.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  val activeHousesState: StateFlow<UiState<List<HouseListing>>> =
    repository
      .observeActiveHouses()
      .map<List<HouseListing>, UiState<List<HouseListing>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreError(error) ?: error
        if (rootError is CancellationException) throw rootError
        Log.w(TAG, "Error observing active houses", rootError)
        emit(UiState.Error(rootError.message ?: "ቤቶችን መጫን አልተቻለም።"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val filteredHousesState: StateFlow<UiState<List<HouseListing>>> =
    combine(activeHousesState, searchQuery, selectedTypeFilter) { state, query, typeFilter ->
        when (state) {
          is UiState.Loading -> UiState.Loading
          is UiState.Error -> state
          is UiState.Success -> {
            val normalizedQuery = query.trim().lowercase()
            val filtered =
              state.data.filter { house ->
                val matchesType = typeFilter == FILTER_ALL || house.type == typeFilter
                val matchesQuery =
                  normalizedQuery.isEmpty() ||
                    house.location.lowercase().contains(normalizedQuery) ||
                    house.title.lowercase().contains(normalizedQuery) ||
                    house.description.lowercase().contains(normalizedQuery)
                matchesType && matchesQuery
              }
            UiState.Success(filtered)
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val myHousesState: StateFlow<UiState<List<HouseListing>>> =
    repository
      .observeMyHouses()
      .map<List<HouseListing>, UiState<List<HouseListing>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreError(error) ?: error
        if (rootError is CancellationException) throw rootError
        Log.w(TAG, "Error observing user houses", rootError)
        emit(UiState.Error(rootError.message ?: "የእርስዎን ቤቶች መጫን አልተቻለም።"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val myUnlocks: StateFlow<Map<String, ContactUnlock>> =
    repository
      .observeMyUnlocks()
      .map { list -> list.associateBy { it.houseId } }
      .catch { error ->
        val rootError = unwrapFirestoreError(error) ?: error
        if (rootError is CancellationException) throw rootError
        Log.w(TAG, "Error observing unlocks", rootError)
        emit(emptyMap())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyMap(),
      )

  init {
    viewModelScope.launch { repository.seedStarterHousesIfEmpty() }
    viewModelScope.launch {
      myUnlocks.collect { unlocks ->
        unlocks.keys.forEach { houseId ->
          if (!_revealedContacts.value.containsKey(houseId)) {
            fetchRevealedContact(houseId)
          }
        }
      }
    }
  }

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  fun onTypeFilterSelected(type: String) {
    _selectedTypeFilter.value = type
  }

  fun openUnlockModal(house: HouseListing) {
    val existingContact = _revealedContacts.value[house.id]
    val existingUnlock = myUnlocks.value[house.id]
    _unlockModalState.value =
      UnlockModalState(
        house = house,
        unlockedContact = existingContact,
        transactionRef = existingUnlock?.transactionRef,
      )
    if (existingContact == null && (existingUnlock != null || (!house.starterListing && house.ownerId == currentUserId))) {
      fetchRevealedContact(house.id)
    }
  }

  fun closeUnlockModal() {
    _unlockModalState.value = null
  }

  fun confirmTelebirrPayment() {
    val currentModal = _unlockModalState.value ?: return
    if (currentModal.isProcessing) return

    _unlockModalState.update { it?.copy(isProcessing = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.unlockHouseContact(currentModal.house.id)
      result
        .onSuccess { contact ->
          _revealedContacts.update { it + (currentModal.house.id to contact) }
          val txRef = myUnlocks.value[currentModal.house.id]?.transactionRef ?: "TB-VERIFIED"
          _unlockModalState.update {
            it?.copy(
              isProcessing = false,
              unlockedContact = contact,
              transactionRef = txRef,
              errorMessage = null,
            )
          }
          _statusMessage.value = "ክፍያው ተሳክቷል! አሁን ከቤቱ ባለቤት ጋር በቀጥታ መደወል ይችላሉ።"
        }
        .onFailure { error ->
          _unlockModalState.update {
            it?.copy(
              isProcessing = false,
              errorMessage = error.localizedMessage ?: "ክፍያውን ማረጋገጥ አልተቻለም። እባክዎ እንደገና ይሞክሩ።",
            )
          }
        }
    }
  }

  fun ensureContactLoaded(house: HouseListing) {
    val isUnlocked =
      myUnlocks.value.containsKey(house.id) || (!house.starterListing && house.ownerId == currentUserId)
    if (isUnlocked && !_revealedContacts.value.containsKey(house.id)) {
      fetchRevealedContact(house.id)
    }
  }

  private fun fetchRevealedContact(houseId: String) {
    viewModelScope.launch {
      repository.getUnlockedContact(houseId).onSuccess { contact ->
        if (contact != null) {
          _revealedContacts.update { it + (houseId to contact) }
          _unlockModalState.update { modal ->
            if (modal?.house?.id == houseId) {
              modal.copy(unlockedContact = contact)
            } else {
              modal
            }
          }
        }
      }
    }
  }

  fun createHouseListing(
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
  ) {
    val cleanTitle = title.trim()
    val cleanLocation = location.trim()
    val cleanPhone = phone.trim()
    val numericPrice = priceInput.filter { it.isDigit() }.toLongOrNull()

    if (cleanTitle.length < 2 || cleanLocation.length < 2 || numericPrice == null || numericPrice <= 0L || cleanPhone.length < 7) {
      _postHouseUiState.value =
        PostHouseUiState(
          isSubmitting = false,
          errorMessage = "እባክዎ ሁሉንም አስፈላጊ መረጃዎች በትክክል ይሙሉ! (ርዕስ፣ ሰፈር፣ ዋጋ እና ስልክ)",
        )
      return
    }

    val formattedNumber = NumberFormat.getNumberInstance(Locale.US).format(numericPrice)
    val priceDisplay =
      if (type == HouseListing.TYPE_RENT) {
        "$formattedNumber ብር / ወር"
      } else {
        "$formattedNumber ብር"
      }
    val areaSqm = areaSqmInput.filter { it.isDigit() }.toLongOrNull()?.coerceIn(0L, 50000L) ?: 0L

    _postHouseUiState.value = PostHouseUiState(isSubmitting = true)
    viewModelScope.launch {
      repository
        .createHouse(
          type = type,
          title = cleanTitle,
          location = cleanLocation,
          priceDisplay = priceDisplay,
          priceAmount = numericPrice,
          bedrooms = bedrooms.toLong().coerceIn(0L, 20L),
          areaSqm = areaSqm,
          description = description.trim(),
          imageKey = imageKey,
          phone = cleanPhone,
          ownerName = ownerName.trim().ifBlank { null },
        )
        .onSuccess { houseId ->
          _postHouseUiState.value =
            PostHouseUiState(isSubmitting = false, successHouseId = houseId)
          _statusMessage.value = "ቤቱ በተሳካ ሁኔታ ተለጥፏል!"
        }
        .onFailure { error ->
          _postHouseUiState.value =
            PostHouseUiState(
              isSubmitting = false,
              errorMessage = error.localizedMessage ?: "ቤቱን መለጠፍ አልተቻለም።",
            )
        }
    }
  }

  fun clearPostSuccess() {
    _postHouseUiState.update { it.copy(successHouseId = null, errorMessage = null) }
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun markListingClosed(houseId: String) {
    viewModelScope.launch {
      repository
        .markHouseClosed(houseId)
        .onSuccess { _statusMessage.value = "ቤቱ ተከራይቷል / ተሽጧል ተብሎ ተዘግቷል።" }
        .onFailure { _statusMessage.value = "ዝርዝሩን ማዘመን አልተቻለም።" }
    }
  }

  fun deleteListing(houseId: String) {
    viewModelScope.launch {
      repository
        .deleteHouse(houseId)
        .onSuccess { _statusMessage.value = "የቤቱ ማስታወቂያ ተሰርዟል።" }
        .onFailure { _statusMessage.value = "ማስታወቂያውን መሰረዝ አልተቻለም።" }
    }
  }

  private fun unwrapFirestoreError(error: Throwable): Throwable? {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    return firestoreError ?: error
  }

  companion object {
    const val FILTER_ALL = "all"
    private const val TAG = "HouseViewModel"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
