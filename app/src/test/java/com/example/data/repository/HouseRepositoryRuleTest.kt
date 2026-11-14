package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.HouseListing
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createHouse_authenticatedOwner_createsListingAndPrivateContact() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = HouseRepository(firestore, auth)

    val result =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createHouse(
          type = HouseListing.TYPE_RENT,
          title = "ባለ 2 መኝታ አፓርታማ",
          location = "ቦሌ",
          priceDisplay = "30,000 ብር / ወር",
          priceAmount = 30000L,
          bedrooms = 2L,
          areaSqm = 105L,
          description = "ዘመናዊ አፓርታማ",
          imageKey = HouseListing.IMAGE_APARTMENT,
          phone = "0911223344",
          ownerName = "Alice",
        )
      }

    assertTrue("Expected success but got: ${result.exceptionOrNull()}", result.isSuccess)
    val houseId = result.getOrThrow()
    assertTrue(houseId.isNotBlank())

    val contact = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getUnlockedContact(houseId).getOrThrow() }
    assertNotNull(contact)
    assertEquals("0911223344", contact?.phone)
  }

  @Test
  fun observeActiveHouses_authenticatedUser_emitsCreatedHouse() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = HouseRepository(firestore, auth)
    val houseId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .createHouse(
            type = HouseListing.TYPE_SALE,
            title = "G+1 ዘመናዊ ቤት",
            location = "አያት",
            priceDisplay = "16,000,000 ብር",
            priceAmount = 16000000L,
            bedrooms = 4L,
            areaSqm = 220L,
            description = "ሰፊ ግቢ",
            imageKey = HouseListing.IMAGE_VILLA,
            phone = "0912000111",
            ownerName = "Alice",
          )
          .getOrThrow()
      }

    val houses =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeActiveHouses().first { list -> list.any { it.id == houseId } }
      }
    assertTrue(houses.any { it.id == houseId && !it.starterListing })
  }

  @Test
  fun getUnlockedContact_crossUserBeforeUnlock_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = HouseRepository(firestore, auth)
    val houseId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createHouse(
            type = HouseListing.TYPE_RENT,
            title = "ኮንዶሚኒየም",
            location = "ሲኤምሲ",
            priceDisplay = "14,000 ብር / ወር",
            priceAmount = 14000L,
            bedrooms = 1L,
            areaSqm = 60L,
            description = "ንጹህ ቤት",
            imageKey = HouseListing.IMAGE_CONDO,
            phone = "0911998877",
            ownerName = "Alice",
          )
          .getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = HouseRepository(firestore, auth)
    val lockedResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUnlockedContact(houseId) }
    assertTrue(lockedResult.isFailure)
    val firestoreException = lockedResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)

    // Bob pays 50 Birr via Telebirr unlock -> now contact is readable
    val unlocked = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.unlockHouseContact(houseId) }
    assertTrue(unlocked.isSuccess)
    assertEquals("0911998877", unlocked.getOrThrow().phone)
  }

  @Test
  fun deleteHouse_nonOwner_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = HouseRepository(firestore, auth)
    val houseId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createHouse(
            type = HouseListing.TYPE_RENT,
            title = "ሰርቪስ ቤት",
            location = "ገርጂ",
            priceDisplay = "8,500 ብር / ወር",
            priceAmount = 8500L,
            bedrooms = 1L,
            areaSqm = 40L,
            description = "ምቹ ቤት",
            imageKey = HouseListing.IMAGE_SERVICE,
            phone = "0913445566",
            ownerName = "Alice",
          )
          .getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = HouseRepository(firestore, auth)
    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deleteHouse(houseId) }
    assertTrue(deleteResult.isFailure)
    val firestoreException = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun observeActiveHouses_unauthenticated_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = HouseRepository(firestore, auth)

    // 1. Repository-level auth guard prevents querying before sign-in
    var repoThrown: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeActiveHouses().first() }
    } catch (t: Throwable) {
      repoThrown = t
    }
    assertTrue("Expected IllegalStateException, got $repoThrown", repoThrown is IllegalStateException)

    // 2. Direct Firestore query while signed out is rejected by security rules with PERMISSION_DENIED
    var ruleThrown: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        firestore
          .collection(HouseRepository.COLLECTION_HOUSES)
          .whereEqualTo("status", HouseListing.STATUS_ACTIVE)
          .snapshots()
          .first()
      }
    } catch (t: Throwable) {
      ruleThrown = t
    }
    assertNotNull(ruleThrown)
    val firestoreException = ruleThrown?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $ruleThrown", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_yaledelala@test.com"
    const val BOB_EMAIL = "bob_yaledelala@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
