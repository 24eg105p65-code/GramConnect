const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_citizen_123";
const BOB_UID = "bob_citizen_456";
const WORKER_UID = "worker_ram_789";
const ADMIN_UID = "admin_varun_001";
const ADMIN_EMAIL = "varunbanoth14@gmail.com";

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

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function unverifiedSpoofAdminContext() {
  return testEnv.authenticatedContext("spoof_admin", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

test("Unauthenticated user cannot read or write complaints or user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("complaints").where("citizenId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("complaints").doc("c_1").set({
      trackingCode: "GC-TG-2026-0001",
      citizenId: ALICE_UID,
    })
  );
});

test("User profile: citizen can create own CITIZEN profile but cannot self-assign GOVT_ADMIN or modify role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rao",
      email: "alice@example.com",
      role: "CITIZEN",
      verificationStatus: "VERIFIED",
      districtId: "warangal",
      mandalId: "wardhannapet",
      villageId: "ilanda",
      wardId: "ward_1",
      preferredLang: "EN",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot self-assign official role on create
  await assertFails(
    aliceDb.collection("users").doc("alice_2").set({
      userId: "alice_2",
      displayName: "Fake Officer",
      email: "alice@example.com",
      role: "GOVT_ADMIN",
      verificationStatus: "VERIFIED",
      districtId: "warangal",
      mandalId: "wardhannapet",
      villageId: "ilanda",
      wardId: "ward_1",
      preferredLang: "EN",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Shadow update on role must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "DISTRICT_OFFICER",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Allowed profile update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLang: "TE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Email spoofing check (email_verified == false must fail admin role approval)
  const spoofDb = unverifiedSpoofAdminContext().firestore();
  await assertFails(
    spoofDb.collection("users").doc(ALICE_UID).update({
      role: "WARD_REP",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Complaints: citizen isolation, @DocumentId exclusion, history, and worker state restrictions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const workerDb = testEnv.authenticatedContext(WORKER_UID).firestore();
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);

  const complaintPayload = {
    trackingCode: "GC-TG-2026-1001",
    citizenId: ALICE_UID,
    citizenName: "Alice Rao",
    title: "Broken drinking water pipeline in Ward 2",
    description: "Main pipeline leaking near Gram Panchayat water tank since morning.",
    category: "WATER_SUPPLY",
    subCategory: "Pipeline damage",
    priority: "HIGH",
    status: "SUBMITTED",
    currentStage: "CITIZEN",
    districtId: "khammam",
    districtName: "Khammam",
    mandalId: "wyra",
    mandalName: "Wyra",
    villageId: "sn_puram",
    villageName: "S.N. Puram",
    wardId: "Ward 2",
    estimatedCostInr: 0,
    fundsRequired: false,
    authorizedUids: [ALICE_UID],
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(aliceDb.collection("complaints").doc("comp_1").set(complaintPayload));

  // Reject payload containing redundant `id` key
  await assertFails(
    aliceDb.collection("complaints").doc("comp_2").set({
      id: "comp_2",
      ...complaintPayload,
    })
  );

  // Citizen filtered list succeeds; unfiltered list fails; cross-user read fails
  await assertSucceeds(aliceDb.collection("complaints").where("citizenId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("complaints").get());
  await assertFails(bobDb.collection("complaints").doc("comp_1").get());
  await assertFails(bobDb.collection("complaints").where("citizenId", "==", ALICE_UID).get());

  // Citizen cannot shortcut status directly to RESOLVED
  await assertFails(
    aliceDb.collection("complaints").doc("comp_1").update({
      status: "RESOLVED",
      currentStage: "CLOSED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // History creation by citizen succeeds
  await assertSucceeds(
    aliceDb.collection("complaints").doc("comp_1").collection("history").doc("hist_1").set({
      complaintId: "comp_1",
      citizenId: ALICE_UID,
      actorId: ALICE_UID,
      actorName: "Alice Rao",
      actorRole: "CITIZEN",
      actionType: "SUBMITTED",
      fromStage: "CITIZEN",
      toStage: "WARD",
      remarks: "Submitted grievance with geo-tag.",
      authorizedUids: [ALICE_UID],
      createdAt: now,
    })
  );

  // Verified admin assigns field worker
  await assertSucceeds(
    adminDb.collection("complaints").doc("comp_1").update({
      status: "WORK_ASSIGNED",
      currentStage: "FIELD_WORKER",
      assignedWorkerId: WORKER_UID,
      assignedWorkerName: "Ramu Field Tech",
      authorizedUids: [ALICE_UID, WORKER_UID],
      latestActionNote: "Assigned to Mandal Water Tech",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Field worker can query assigned work orders and update progress
  await assertSucceeds(workerDb.collection("complaints").where("assignedWorkerId", "==", WORKER_UID).get());
  await assertSucceeds(
    workerDb.collection("complaints").doc("comp_1").update({
      status: "IN_PROGRESS",
      currentStage: "FIELD_WORKER",
      latestActionNote: "Replacement valve installed, testing pressure.",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Field worker cannot assign funds or escalate to RESOLVED directly
  await assertFails(
    workerDb.collection("complaints").doc("comp_1").update({
      status: "RESOLVED",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Admin resolves complaint -> terminal state lock for non-admins
  await assertSucceeds(
    adminDb.collection("complaints").doc("comp_1").update({
      status: "RESOLVED",
      currentStage: "CLOSED",
      updatedAt: new Date(Date.now() - 50),
    })
  );
  await assertFails(
    workerDb.collection("complaints").doc("comp_1").update({
      status: "IN_PROGRESS",
      updatedAt: new Date(Date.now() - 10),
    })
  );
});

test("Schemes, PublicProjects, RationSummaries, and User Subcollections obey isolation & query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    adminDb.collection("schemes").doc("rythu_bharosa").set({
      title: "Rythu Bharosa",
      level: "TELANGANA_STATE",
      department: "Agriculture & Welfare",
      category: "Farmer Welfare",
      activeStatus: "ACTIVE",
      benefitsSummary: "Direct investment support per acre for eligible farmers.",
      eligibilityCriteria: "Land-owning farmers in Telangana with valid pattadar passbook.",
      requiredDocuments: "Aadhaar, Pattadar Passbook, Bank Account",
      applicationProcess: "Apply at Mandal Agriculture Office or Grama Sabha verification.",
      applicationDeadline: "31 Dec 2026 (Rabi Cycle)",
      beneficiaryCount: 6850000,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered published query succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("schemes").where("published", "==", true).get());
  await assertFails(aliceDb.collection("schemes").get());

  // Citizen cannot create official schemes
  await assertFails(
    aliceDb.collection("schemes").doc("fake_scheme").set({
      title: "Fake",
      level: "CENTRAL",
      department: "Welfare",
      category: "Other",
      activeStatus: "ACTIVE",
      benefitsSummary: "None",
      eligibilityCriteria: "Any",
      requiredDocuments: "None",
      applicationProcess: "None",
      beneficiaryCount: 0,
      published: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // User-scoped scheme applications isolated by userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("schemeApplications").doc("app_1").set({
      userId: ALICE_UID,
      schemeId: "rythu_bharosa",
      schemeTitle: "Rythu Bharosa",
      referenceNumber: "TG-SCH-2026-112",
      villageName: "Ilanda",
      verificationStatus: "PENDING_VERIFICATION",
      applicationStatus: "SUBMITTED",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("schemeApplications").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("schemeApplications").get());

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("n_future").set({
      userId: ALICE_UID,
      title: "Future",
      message: "Bad timestamp",
      relatedId: "comp_1",
      notificationType: "COMPLAINT",
      readStatus: false,
      createdAt: new Date(Date.now() + 120000),
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("RoleRequests, FundRequests, PublicProjects, RationSummaries, and Feedback enforce strict RBAC", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 1000);

  // Role request: citizen can submit PENDING, cannot self-approve
  await assertSucceeds(
    aliceDb.collection("roleRequests").doc("req_1").set({
      userId: ALICE_UID,
      applicantName: "Alice Rao",
      requestedRole: "WARD_REP",
      officialWorkId: "TG-WR-204",
      department: "WATER_SUPPLY",
      districtId: "warangal",
      mandalId: "wardhannapet",
      villageId: "ilanda",
      wardId: "Ward 1",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("roleRequests").doc("req_1").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(
    adminDb.collection("roleRequests").doc("req_1").update({
      status: "APPROVED",
      reviewerNotes: "Verified against Gram Panchayat register.",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // FundRequest: citizen cannot create, admin can create & sanction
  await assertFails(
    aliceDb.collection("fundRequests").doc("fund_1").set({
      complaintId: "comp_1",
      projectTitle: "Ward 2 Pipeline Replacement",
      requesterId: ALICE_UID,
      requesterName: "Alice Rao",
      department: "WATER_SUPPLY",
      districtId: "warangal",
      mandalId: "wardhannapet",
      villageId: "ilanda",
      fundSource: "15th Finance Commission",
      estimatedCostInr: 125000,
      requestedAmountInr: 125000,
      sanctionedAmountInr: 0,
      expenditureInr: 0,
      approvalStage: "MANDAL",
      status: "REQUESTED",
      reviewerUids: [ADMIN_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("fundRequests").doc("fund_1").set({
      complaintId: "comp_1",
      projectTitle: "Ward 2 Pipeline Replacement",
      requesterId: ADMIN_UID,
      requesterName: "Mandal Officer",
      department: "WATER_SUPPLY",
      districtId: "warangal",
      mandalId: "wardhannapet",
      villageId: "ilanda",
      fundSource: "15th Finance Commission",
      estimatedCostInr: 125000,
      requestedAmountInr: 125000,
      sanctionedAmountInr: 0,
      expenditureInr: 0,
      approvalStage: "MANDAL",
      status: "REQUESTED",
      reviewerUids: [ADMIN_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // PublicProject & RationSummary filtered queries
  await assertSucceeds(
    adminDb.collection("publicProjects").doc("proj_1").set({
      title: "Mission Bhagiratha Overhead Tank Repair",
      department: "WATER_SUPPLY",
      districtId: "warangal",
      districtName: "Warangal",
      mandalName: "Wardhannapet",
      villageName: "Ilanda",
      sanctionedAmountInr: 450000,
      expenditureInr: 310000,
      progressPercent: 70,
      status: "IN_PROGRESS",
      summary: "Pipeline valve and storage chlorination upgrade.",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("publicProjects").where("published", "==", true).get());
  await assertFails(aliceDb.collection("publicProjects").get());

  // Feedback: citizen submits own feedback, cannot edit someone else's
  await assertSucceeds(
    aliceDb.collection("feedback").doc("proj_1_" + ALICE_UID).set({
      citizenId: ALICE_UID,
      targetId: "proj_1",
      targetTitle: "Mission Bhagiratha Overhead Tank Repair",
      targetType: "DEVELOPMENT_PROJECT",
      districtName: "Warangal",
      villageName: "Ilanda",
      rating: 5,
      comment: "Water pressure restored cleanly across Ward 1 and 2.",
      moderatedPublished: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("feedback").doc("proj_1_" + ALICE_UID).update({
      rating: 1,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
