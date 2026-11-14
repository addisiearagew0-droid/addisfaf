const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validHousePayload(ownerId, overrides = {}) {
  return {
    ownerId,
    type: "ኪራይ",
    title: "ባለ 2 መኝታ ዘመናዊ አፓርታማ",
    location: "ቦሌ መድኃኔዓለም",
    priceDisplay: "35,000 ብር / ወር",
    priceAmount: 35000,
    bedrooms: 2,
    areaSqm: 110,
    description: "ንጹህ እና ዘመናዊ አፓርታማ",
    imageKey: "apartment",
    status: "active",
    starterListing: false,
    createdAt: nowTs(),
    updatedAt: nowTs(),
    ...overrides,
  };
}

function validContactPayload(ownerId, overrides = {}) {
  return {
    ownerId,
    phone: "0911234567",
    ownerName: "Abebe Kebede",
    createdAt: nowTs(),
    updatedAt: nowTs(),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot list or read houses or contacts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("houses").where("status", "==", "active").get());
  await assertFails(unauthDb.collection("houses").doc("h1").get());
  await assertFails(unauthDb.collection("houses").doc("h1").collection("contact").doc("private").get());
});

test("Authenticated user: unfiltered houses list fails (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("houses").get());
});

test("Authenticated user: filtered active houses query succeeds", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("houses").doc("house_1").set(validHousePayload(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("houses").where("status", "==", "active").get());
});

test("Authenticated user: atomic batch create of house + private contact succeeds for owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const houseRef = aliceDb.collection("houses").doc("house_alice_1");
  const contactRef = houseRef.collection("contact").doc("private");
  batch.set(houseRef, validHousePayload(ALICE_UID));
  batch.set(contactRef, validContactPayload(ALICE_UID));
  await assertSucceeds(batch.commit());
});

test("Identity spoofing: cannot create house with another user's ownerId", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("houses").doc("spoof_1").set(validHousePayload(ALICE_UID))
  );
});

test("Shadow update test: ghost field rejected on house create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("houses").doc("ghost_1").set(validHousePayload(ALICE_UID, { isVerified: true }))
  );

  const batch = aliceDb.batch();
  const houseRef = aliceDb.collection("houses").doc("house_alice_2");
  batch.set(houseRef, validHousePayload(ALICE_UID));
  batch.set(houseRef.collection("contact").doc("private"), validContactPayload(ALICE_UID));
  await assertSucceeds(batch.commit());

  await assertFails(houseRef.update({ isVerified: true, updatedAt: nowTs() }));
});

test("PII Isolation: Bob CANNOT read Alice's private contact before unlocking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const houseRef = aliceDb.collection("houses").doc("house_pii_1");
  batch.set(houseRef, validHousePayload(ALICE_UID));
  batch.set(houseRef.collection("contact").doc("private"), validContactPayload(ALICE_UID));
  await assertSucceeds(batch.commit());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("houses").doc("house_pii_1").collection("contact").doc("private").get());
});

test("PII Unlock Flow: Bob unlocks with 50 ETB Telebirr receipt, then CAN read private contact", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const houseRef = aliceDb.collection("houses").doc("house_unlock_1");
  batch.set(houseRef, validHousePayload(ALICE_UID));
  batch.set(houseRef.collection("contact").doc("private"), validContactPayload(ALICE_UID));
  await assertSucceeds(batch.commit());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("unlocks").doc("house_unlock_1").set({
      userId: BOB_UID,
      houseId: "house_unlock_1",
      amountBirr: 50,
      paymentMethod: "telebirr",
      transactionRef: "TB-982341",
      createdAt: nowTs(),
    })
  );

  await assertSucceeds(bobDb.collection("houses").doc("house_unlock_1").collection("contact").doc("private").get());
});

test("Cross-user isolation: Bob cannot write unlock under Alice's path or delete Alice's house", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const houseRef = aliceDb.collection("houses").doc("house_iso_1");
  batch.set(houseRef, validHousePayload(ALICE_UID));
  batch.set(houseRef.collection("contact").doc("private"), validContactPayload(ALICE_UID));
  await assertSucceeds(batch.commit());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("unlocks").doc("house_iso_1").set({
      userId: ALICE_UID,
      houseId: "house_iso_1",
      amountBirr: 50,
      paymentMethod: "telebirr",
      transactionRef: "TB-111111",
      createdAt: nowTs(),
    })
  );
  await assertFails(bobDb.collection("houses").doc("house_iso_1").delete());
});

test("Terminal state locking: closed house cannot be reopened or edited", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const houseRef = aliceDb.collection("houses").doc("house_term_1");
  batch.set(houseRef, validHousePayload(ALICE_UID));
  batch.set(houseRef.collection("contact").doc("private"), validContactPayload(ALICE_UID));
  await assertSucceeds(batch.commit());

  await assertSucceeds(houseRef.update({ status: "closed", updatedAt: nowTs() }));
  await assertFails(houseRef.update({ status: "active", updatedAt: nowTs() }));
  await assertFails(houseRef.update({ title: "Updated Title", updatedAt: nowTs() }));
});
