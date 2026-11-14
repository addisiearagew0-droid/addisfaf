package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ContactUnlock
import com.example.data.model.HouseContact
import com.example.data.model.HouseListing
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HouseRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  fun observeActiveHouses(): Flow<List<HouseListing>> = flow {
    val uid = requireUserId()
    val path = COLLECTION_HOUSES
    emitAll(
      db.collection(COLLECTION_HOUSES)
        .whereEqualTo("status", HouseListing.STATUS_ACTIVE)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              HouseListing::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeMyHouses(): Flow<List<HouseListing>> = flow {
    val uid = requireUserId()
    val path = COLLECTION_HOUSES
    emitAll(
      db.collection(COLLECTION_HOUSES)
        .whereEqualTo("ownerId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              HouseListing::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .filter { !it.starterListing }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  fun observeMyUnlocks(): Flow<List<ContactUnlock>> = flow {
    val uid = requireUserId()
    val path = "$COLLECTION_USERS/$uid/$COLLECTION_UNLOCKS"
    emitAll(
      db.collection(COLLECTION_USERS)
        .document(uid)
        .collection(COLLECTION_UNLOCKS)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            ContactUnlock::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, uid)
          throw error
        }
    )
  }

  suspend fun seedStarterHousesIfEmpty(): Result<Unit> {
    val uid =
      auth.currentUser?.uid
        ?: return Result.failure(IllegalStateException("User must be signed in."))
    return try {
      val existing =
        db.collection(COLLECTION_HOUSES)
          .whereEqualTo("status", HouseListing.STATUS_ACTIVE)
          .limit(1)
          .get()
          .await()
      if (!existing.isEmpty) {
        return Result.success(Unit)
      }

      val batch = db.batch()
      for (starter in STARTER_HOUSES) {
        val houseRef = db.collection(COLLECTION_HOUSES).document(starter.id)
        val contactRef = houseRef.collection(COLLECTION_CONTACT).document(DOC_PRIVATE)
        val housePayload =
          mapOf(
            "ownerId" to uid,
            "type" to starter.type,
            "title" to starter.title,
            "location" to starter.location,
            "priceDisplay" to starter.priceDisplay,
            "priceAmount" to starter.priceAmount,
            "bedrooms" to starter.bedrooms,
            "areaSqm" to starter.areaSqm,
            "description" to starter.description,
            "imageKey" to starter.imageKey,
            "status" to HouseListing.STATUS_ACTIVE,
            "starterListing" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        val contactPayload =
          mapOf(
            "ownerId" to uid,
            "phone" to starter.phone,
            "ownerName" to starter.ownerName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        batch.set(houseRef, housePayload)
        batch.set(contactRef, contactPayload)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, COLLECTION_HOUSES, auth)
      Result.failure(e)
    }
  }

  suspend fun createHouse(
    type: String,
    title: String,
    location: String,
    priceDisplay: String,
    priceAmount: Long,
    bedrooms: Long,
    areaSqm: Long,
    description: String,
    imageKey: String,
    phone: String,
    ownerName: String?,
  ): Result<String> {
    val uid =
      auth.currentUser?.uid
        ?: return Result.failure(IllegalStateException("User must be signed in."))
    val houseRef = db.collection(COLLECTION_HOUSES).document()
    val contactRef = houseRef.collection(COLLECTION_CONTACT).document(DOC_PRIVATE)

    val housePayload =
      mapOf(
        "ownerId" to uid,
        "type" to type.trim(),
        "title" to title.trim(),
        "location" to location.trim(),
        "priceDisplay" to priceDisplay.trim(),
        "priceAmount" to priceAmount.coerceAtLeast(0L),
        "bedrooms" to bedrooms.coerceIn(0L, 20L),
        "areaSqm" to areaSqm.coerceIn(0L, 50000L),
        "description" to description.trim(),
        "imageKey" to imageKey,
        "status" to HouseListing.STATUS_ACTIVE,
        "starterListing" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    val contactPayload =
      buildMap<String, Any> {
        put("ownerId", uid)
        put("phone", phone.trim())
        if (!ownerName.isNullOrBlank()) {
          put("ownerName", ownerName.trim())
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }

    return try {
      val batch = db.batch()
      batch.set(houseRef, housePayload)
      batch.set(contactRef, contactPayload)
      batch.commit().await()
      Result.success(houseRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, houseRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun unlockHouseContact(houseId: String): Result<HouseContact> {
    val uid =
      auth.currentUser?.uid
        ?: return Result.failure(IllegalStateException("User must be signed in."))
    val unlockRef =
      db.collection(COLLECTION_USERS)
        .document(uid)
        .collection(COLLECTION_UNLOCKS)
        .document(houseId)
    val contactRef =
      db.collection(COLLECTION_HOUSES)
        .document(houseId)
        .collection(COLLECTION_CONTACT)
        .document(DOC_PRIVATE)

    val txRef = "TB-${UUID.randomUUID().toString().take(8).uppercase()}"
    val unlockPayload =
      mapOf(
        "userId" to uid,
        "houseId" to houseId,
        "amountBirr" to UNLOCK_FEE_BIRR,
        "paymentMethod" to PAYMENT_METHOD_TELEBIRR,
        "transactionRef" to txRef,
        "createdAt" to FieldValue.serverTimestamp(),
      )

    return try {
      val existingUnlock = unlockRef.get().await()
      if (!existingUnlock.exists()) {
        unlockRef.set(unlockPayload).await()
      }
      val contactSnap = contactRef.get().await()
      val contact =
        contactSnap.toObject(
          HouseContact::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
          ?: return Result.failure(
            IllegalStateException("Contact details not found for listing $houseId")
          )
      Result.success(contact)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, unlockRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun getUnlockedContact(houseId: String): Result<HouseContact?> {
    val contactRef =
      db.collection(COLLECTION_HOUSES)
        .document(houseId)
        .collection(COLLECTION_CONTACT)
        .document(DOC_PRIVATE)
    return try {
      val snapshot = contactRef.get().await()
      val contact =
        if (snapshot.exists()) {
          snapshot.toObject(
            HouseContact::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      Result.success(contact)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, contactRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun markHouseClosed(houseId: String): Result<Unit> {
    val docRef = db.collection(COLLECTION_HOUSES).document(houseId)
    return try {
      docRef
        .update(
          mapOf(
            "status" to HouseListing.STATUS_CLOSED,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deleteHouse(houseId: String): Result<Unit> {
    val houseRef = db.collection(COLLECTION_HOUSES).document(houseId)
    val contactRef = houseRef.collection(COLLECTION_CONTACT).document(DOC_PRIVATE)
    return try {
      val batch = db.batch()
      batch.delete(contactRef)
      batch.delete(houseRef)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, houseRef.path, auth)
      Result.failure(e)
    }
  }

  private fun logFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: error
    if (auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, operationType, path, auth)
    }
  }

  private data class StarterHouseSeed(
    val id: String,
    val type: String,
    val title: String,
    val location: String,
    val priceDisplay: String,
    val priceAmount: Long,
    val bedrooms: Long,
    val areaSqm: Long,
    val description: String,
    val imageKey: String,
    val phone: String,
    val ownerName: String,
  )

  companion object {
    const val COLLECTION_HOUSES = "houses"
    const val COLLECTION_CONTACT = "contact"
    const val DOC_PRIVATE = "private"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_UNLOCKS = "unlocks"
    const val UNLOCK_FEE_BIRR = 50L
    const val PAYMENT_METHOD_TELEBIRR = "telebirr"

    private val STARTER_HOUSES =
      listOf(
        StarterHouseSeed(
          id = "starter_bole_apt",
          type = HouseListing.TYPE_RENT,
          title = "ባለ 2 መኝታ ዘመናዊ አፓርታማ",
          location = "ቦሌ መድኃኔዓለም",
          priceDisplay = "35,000 ብር / ወር",
          priceAmount = 35000L,
          bedrooms = 2L,
          areaSqm = 115L,
          description = "ሊፍት፣ ጀነሬተር፣ የመኪና ማቆሚያ እና 24 ሰዓት ጥበቃ ያለው ዘመናዊ አፓርታማ።",
          imageKey = HouseListing.IMAGE_APARTMENT,
          phone = "0911-23-45-67",
          ownerName = "አቶ ዳዊት ተስፋዬ",
        ),
        StarterHouseSeed(
          id = "starter_cmc_condo",
          type = HouseListing.TYPE_RENT,
          title = "ባለ 1 መኝታ ኮንዶሚኒየም (3ኛ ፎቅ)",
          location = "ሲኤምሲ (CMC)",
          priceDisplay = "15,000 ብር / ወር",
          priceAmount = 15000L,
          bedrooms = 1L,
          areaSqm = 62L,
          description = "ለትራንስፖርት እና ለሱቆች ቅርብ የሆነ፣ ውሃና መብራት የተሟላለት ንጹህ ኮንዶሚኒየም።",
          imageKey = HouseListing.IMAGE_CONDO,
          phone = "0911-45-67-89",
          ownerName = "ወ/ሮ ሰላም አበበ",
        ),
        StarterHouseSeed(
          id = "starter_ayat_g1",
          type = HouseListing.TYPE_SALE,
          title = "G+1 ዘመናዊ መኖሪያ ቤት (250 ካሬ)",
          location = "አያት",
          priceDisplay = "18,500,000 ብር",
          priceAmount = 18500000L,
          bedrooms = 4L,
          areaSqm = 250L,
          description = "ሰፊ ግቢ፣ 4 መኝታ ክፍሎች፣ ዘመናዊ ኪችን እና የተሟላ ካርታ ያለው መኖሪያ ቤት።",
          imageKey = HouseListing.IMAGE_VILLA,
          phone = "0912-34-56-78",
          ownerName = "አቶ ብርሃኑ መኮንን",
        ),
        StarterHouseSeed(
          id = "starter_gerji_service",
          type = HouseListing.TYPE_RENT,
          title = "ሰርቪስ ቤት (1 ክፍል እና ሳሎን)",
          location = "ገርጂ",
          priceDisplay = "9,000 ብር / ወር",
          priceAmount = 9000L,
          bedrooms = 1L,
          areaSqm = 45L,
          description = "ጸጥታ የሰፈነበት ግቢ ውስጥ፣ የራሱ መታጠቢያ እና ኪችን ያለው ምቹ ሰርቪስ ቤት።",
          imageKey = HouseListing.IMAGE_SERVICE,
          phone = "0913-98-76-54",
          ownerName = "ወ/ሮ ትግስት ኃይሉ",
        ),
      )
  }
}
