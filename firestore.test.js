const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile: Alice can create and read her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Driver",
      email: "alice@test.com",
      vehicleName: "White Thar",
      vehicleColor: "#2196F3",
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  await assertSucceeds(profileRef.get());
});

test("User profile: Alice cannot read Bob's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Driver",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Trips: Alice can create a trip and list trips where she is participant", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tripRef = aliceDb.collection("trips").doc("trip_1");

  await assertSucceeds(
    tripRef.set({
      tripId: "trip_1",
      title: "Manali Road Trip",
      destinationName: "Manali",
      destinationLat: 32.2396,
      destinationLng: 77.1887,
      originName: "Delhi",
      originLat: 28.6139,
      originLng: 77.2090,
      creatorId: ALICE_UID,
      joinCode: "MANALI",
      status: "active",
      participantIds: [ALICE_UID],
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  // List query with participantIds filter
  await assertSucceeds(
    aliceDb.collection("trips").where("participantIds", "array-contains", ALICE_UID).get()
  );
});

test("Vehicle Location: Alice can write telemetry to her vehicle in trip", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trips").doc("trip_1").set({
      tripId: "trip_1",
      title: "Goa Highway Convoy",
      destinationName: "Goa",
      destinationLat: 15.2993,
      destinationLng: 74.1240,
      creatorId: ALICE_UID,
      joinCode: "GOA001",
      status: "active",
      participantIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const vehicleRef = aliceDb.collection("trips").doc("trip_1").collection("vehicles").doc(ALICE_UID);

  await assertSucceeds(
    vehicleRef.set({
      vehicleId: ALICE_UID,
      tripId: "trip_1",
      driverName: "Alice",
      vehicleName: "Red SUV",
      vehicleColor: "#E53935",
      latitude: 18.5204,
      longitude: 73.8567,
      speedKmh: 65.0,
      heading: 180.0,
      batteryPercent: 88.0,
      status: "moving",
      updatedAt: new Date()
    })
  );
});

test("Vehicle Location: Bob can read Alice's vehicle telemetry as co-participant", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trips").doc("trip_1").set({
      tripId: "trip_1",
      title: "Goa Highway Convoy",
      destinationName: "Goa",
      destinationLat: 15.2993,
      destinationLng: 74.1240,
      creatorId: ALICE_UID,
      joinCode: "GOA001",
      status: "active",
      participantIds: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date()
    });

    await context.firestore().collection("trips").doc("trip_1").collection("vehicles").doc(ALICE_UID).set({
      vehicleId: ALICE_UID,
      tripId: "trip_1",
      driverName: "Alice",
      latitude: 18.5204,
      longitude: 73.8567,
      speedKmh: 65.0,
      heading: 180.0,
      status: "moving",
      updatedAt: new Date()
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("trips").doc("trip_1").collection("vehicles").doc(ALICE_UID).get()
  );

  // Non-participant Charlie cannot read
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("trips").doc("trip_1").collection("vehicles").doc(ALICE_UID).get()
  );
});
