const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_vol_1";
const BOB_UID = "bob_vol_2";
const OFFICE_UID = "office_coord_1";
const ADMIN_UID = "admin_root_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = () => new Date(Date.now() - 2000);

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

async function seedStaffAndOpportunity() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = pastDate();
    await db.collection("office_team").doc(OFFICE_UID).set({
      userId: OFFICE_UID,
      displayName: "Karthik Office",
      email: "karthik@maatram.org",
      department: "Admissions & PV",
      canApproveHours: true,
      canAllocatePv: true,
      canManageEvents: true,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("opportunities").doc("opp_pv_trichy").set({
      title: "Tiruchirappalli Family PV Drive",
      category: "PHYSICAL_VERIFICATION",
      activityType: "Physical Verification",
      description: "Home visits for 2026 scholarship applicants",
      district: "Tiruchirappalli",
      location: "Srirangam & Woraiyur Zones",
      mode: "OFFLINE",
      eventDate: "2026-12-05",
      startTime: "09:00",
      endTime: "16:00",
      deadline: "2026-12-02",
      requiredVolunteers: 4,
      availableSlots: 4,
      totalCases: 8,
      skillsRequired: "Local travel, empathy",
      eligibility: "Active Maatram Volunteer",
      instructions: "Carry ID badge and verify family background respectfully.",
      whatsappGroupLink: "https://chat.whatsapp.com/demo-trichy-pv",
      status: "OPEN",
      multiDayCount: 1,
      createdBy: OFFICE_UID,
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated users are denied across all collections", async () => {
  await seedStaffAndOpportunity();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("opportunities").get());
  await assertFails(unauthDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("volunteer_stats").get());
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("Authenticated volunteer can list catalog collections with Kotlin-aligned queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("opportunities").where("status", "in", ["OPEN", "CLOSED", "COMPLETED"]).get());
  await assertSucceeds(aliceDb.collection("volunteer_stats").where("points", ">=", 0).get());
  await assertSucceeds(aliceDb.collection("districts").where("active", "==", true).get());
  await assertSucceeds(aliceDb.collection("organizations").where("active", "==", true).get());
  await assertSucceeds(aliceDb.collection("app_settings").doc("global").get());
  await assertFails(aliceDb.collection("opportunities").get());
  await assertFails(aliceDb.collection("app_settings").get());
});

test("Profile PII isolation & anti-privilege-escalation", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  // Alice creates her own volunteer profile
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sundaram",
      email: "alice@example.com",
      phone: "9876500001",
      volunteerCode: "MF-2026-101",
      volunteerType: "STUDENT",
      college: "NIT Tiruchirappalli",
      department: "CSE",
      yearOfStudy: "3rd Year",
      district: "Tiruchirappalli",
      city: "Trichy",
      skills: "Communication, Tamil",
      interests: "Physical Verification",
      availability: "Weekends",
      preferredContact: "WhatsApp",
      employerOrgId: "org_student",
      employerOrgName: "Maatram Student Chapter",
      consentDataProcessing: true,
      consentLinkedInPublic: false,
      profileCompleted: true,
      role: "VOLUNTEER",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice cannot self-escalate role to ADMIN
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read Alice's private profile (PII)
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(bobDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("profiles").get());

  // Office team CAN read Alice's profile
  const officeDb = testEnv.authenticatedContext(OFFICE_UID).firestore();
  await seedStaffAndOpportunity();
  await assertSucceeds(officeDb.collection("profiles").doc(ALICE_UID).get());
});

test("Volunteer stats prevent self-manipulation of verified hours & points", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("volunteer_stats").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Sundaram",
      volunteerCode: "MF-2026-101",
      volunteerType: "STUDENT",
      district: "Tiruchirappalli",
      employerOrgName: "Maatram Student Chapter",
      availability: "Weekends",
      availableForPv: true,
      verifiedHours: 0,
      completedActivities: 0,
      completedPvCases: 0,
      activePvCases: 0,
      points: 0,
      consistencyScore: 0,
      tier: "BRONZE",
      badges: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Volunteer CANNOT inflate own verifiedHours or points
  await assertFails(
    aliceDb.collection("volunteer_stats").doc(ALICE_UID).update({
      verifiedHours: 50,
      points: 500,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Volunteer CAN toggle availability
  await assertSucceeds(
    aliceDb.collection("volunteer_stats").doc(ALICE_UID).update({
      availableForPv: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Volunteer requests, query alignment, and Office Team approval", async () => {
  await seedStaffAndOpportunity();
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const officeDb = testEnv.authenticatedContext(OFFICE_UID).firestore();

  // Alice submits join request
  await assertSucceeds(
    aliceDb.collection("volunteer_requests").doc("opp_pv_trichy_" + ALICE_UID).set({
      userId: ALICE_UID,
      volunteerName: "Alice Sundaram",
      volunteerCode: "MF-2026-101",
      opportunityId: "opp_pv_trichy",
      opportunityTitle: "Tiruchirappalli Family PV Drive",
      category: "PHYSICAL_VERIFICATION",
      district: "Tiruchirappalli",
      status: "PENDING",
      availabilityNote: "Available Saturday full day",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice cannot self-approve
  await assertFails(
    aliceDb.collection("volunteer_requests").doc("opp_pv_trichy_" + ALICE_UID).update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Filtered query succeeds, unfiltered query fails for volunteer
  await assertSucceeds(
    aliceDb.collection("volunteer_requests").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("volunteer_requests").get());

  // Office Team approves request
  await assertSucceeds(
    officeDb.collection("volunteer_requests").doc("opp_pv_trichy_" + ALICE_UID).update({
      status: "APPROVED",
      reviewedBy: OFFICE_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Physical verification cases & hours verification enforcement", async () => {
  await seedStaffAndOpportunity();
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const officeDb = testEnv.authenticatedContext(OFFICE_UID).firestore();

  await assertSucceeds(
    officeDb.collection("pv_cases").doc("case_1").set({
      caseRefId: "PV-TRY-2026-001",
      opportunityId: "opp_pv_trichy",
      district: "Tiruchirappalli",
      areaLabel: "Srirangam Zone",
      applicantCode: "APP-TRY-901",
      assignedToUid: ALICE_UID,
      assignedToName: "Alice Sundaram",
      status: "ASSIGNED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot view Alice's assigned case
  await assertFails(bobDb.collection("pv_cases").doc("case_1").get());

  // Alice can update visit progress on her case
  await assertSucceeds(
    aliceDb.collection("pv_cases").doc("case_1").update({
      status: "COMPLETED",
      visitNotes: "Family verified at Srirangam.",
      recommendationSummary: "Recommended for full scholarship",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice cannot mark her own case VERIFIED
  await assertFails(
    aliceDb.collection("pv_cases").doc("case_1").update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Alice submits pending volunteer hours
  await assertSucceeds(
    aliceDb.collection("volunteer_hours").doc("hr_1").set({
      userId: ALICE_UID,
      volunteerName: "Alice Sundaram",
      volunteerCode: "MF-2026-101",
      assignmentId: "opp_pv_trichy_" + ALICE_UID,
      opportunityId: "opp_pv_trichy",
      opportunityTitle: "Tiruchirappalli Family PV Drive",
      category: "PHYSICAL_VERIFICATION",
      district: "Tiruchirappalli",
      employerOrgName: "Maatram Student Chapter",
      activityDate: "2026-12-05",
      startTime: "09:00",
      endTime: "13:30",
      breakMinutes: 30,
      calculatedHours: 4.0,
      submittedHours: 4.0,
      dayNumber: 1,
      notes: "Completed home verification visit",
      verificationStatus: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice cannot self-approve hours
  await assertFails(
    aliceDb.collection("volunteer_hours").doc("hr_1").update({
      verificationStatus: "APPROVED",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Office Team approves hours
  await assertSucceeds(
    officeDb.collection("volunteer_hours").doc("hr_1").update({
      verificationStatus: "APPROVED",
      reviewerUid: OFFICE_UID,
      reviewerName: "Karthik Office",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Bootstrapped Admin email verification & immutable audit logs", async () => {
  const ts = pastDate();

  // Spoofed unverified admin email MUST fail
  const spoofDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "nrajasri99@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("districts").doc("trichy").set({
      name: "Tiruchirappalli",
      code: "TRY",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified bootstrapped admin succeeds
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "nrajasri99@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("districts").doc("trichy").set({
      name: "Tiruchirappalli",
      code: "TRY",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Audit logs can be created, never updated or deleted
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      actorUid: ADMIN_UID,
      actorName: "Rajasri Admin",
      actorRole: "ADMIN",
      action: "DISTRICT_CREATED",
      targetType: "districts",
      targetId: "trichy",
      details: "Added Tiruchirappalli district",
      createdAt: ts,
    })
  );
  await assertFails(
    adminDb.collection("audit_logs").doc("log_1").update({
      details: "Tampered",
    })
  );
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());
});
