const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCreatePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    company: "Demo Textile Co.",
    role: "Production / Machine Operator",
    region: "europe",
    country: "Italy",
    city: "Milan",
    date: "2026-11-20",
    followDate: "2026-12-05",
    cvSet: "Europe",
    cvFile: "Textile CV",
    website: "https://demo-textile.example",
    email: "hr@demo-textile.example",
    confirmCode: "IT-TX-204",
    status: "applied",
    notes: "Test note",
    demoEntry: false,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated user cannot read or write any user applications", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("applications").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("applications").doc("app_1").set(
      validCreatePayload(ALICE_UID)
    )
  );
});

test("Authenticated owner can create, list, partially update status, full update, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("applications");

  await assertSucceeds(col.doc("app_1").set(validCreatePayload(ALICE_UID)));
  await assertSucceeds(col.orderBy("createdAt", "desc").get());

  // Status-only partial update
  await assertSucceeds(
    col.doc("app_1").update({
      status: "followup",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Full editable fields update
  await assertSucceeds(
    col.doc("app_1").update({
      company: "Demo Textile Updated",
      role: "Senior Operator",
      region: "europe",
      country: "Italy",
      city: "Turin",
      date: "2026-11-20",
      followDate: "2026-12-06",
      cvSet: "Europe",
      cvFile: "Textile CV",
      website: "https://demo-textile.example",
      email: "hr@demo-textile.example",
      confirmCode: "IT-TX-205",
      status: "interview",
      notes: "Updated",
      demoEntry: false,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertSucceeds(col.doc("app_1").delete());
});

test("Cross-user access (Bob reading/writing/deleting Alice's applications) is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("applications").doc("app_1")
      .set(validCreatePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceCol = bobDb.collection("users").doc(ALICE_UID).collection("applications");

  await assertFails(aliceCol.get());
  await assertFails(aliceCol.doc("app_1").get());
  await assertFails(aliceCol.doc("app_1").update({ status: "rejected", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceCol.doc("app_1").delete());
  await assertFails(aliceCol.doc("app_2").set(validCreatePayload(BOB_UID)));
});

test("Rejects ghost fields, @DocumentId 'id' in payload, immutable createdAt/userId edits, and invalid status/future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("applications");

  await assertSucceeds(col.doc("app_1").set(validCreatePayload(ALICE_UID)));

  // Ghost field rejected
  await assertFails(col.doc("app_2").set(validCreatePayload(ALICE_UID, { isAdmin: true })));

  // Redundant @DocumentId 'id' key rejected
  await assertFails(col.doc("app_3").set(validCreatePayload(ALICE_UID, { id: "app_3" })));

  // Invalid status rejected
  await assertFails(col.doc("app_4").set(validCreatePayload(ALICE_UID, { status: "unknown" })));

  // Future timestamp rejected
  await assertFails(
    col.doc("app_5").set(validCreatePayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    col.doc("app_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Immutable userId mutation rejected
  await assertFails(
    col.doc("app_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
