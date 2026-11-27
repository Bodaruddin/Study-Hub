const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const STUDENT_A = "student_alice";
const STUDENT_B = "student_bob";
const ADMIN_UID = "admin_user";
const ADMIN_EMAIL = "momrooja@gmail.com";

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

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(STUDENT_A).get());
  await assertFails(unauth.collection("study_materials").where("published", "==", true).get());
  await assertFails(unauth.collection("quizzes").where("published", "==", true).get());
});

test("User profile isolation, self-registration, and anti-privilege-escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_A, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_B, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = pastDate();

  // Valid student profile creation
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).set({
      userId: STUDENT_A,
      displayName: "Alice",
      email: "alice@example.com",
      selectedClass: 7,
      role: "student",
      subscriptionTier: "free",
      avgScore: 0,
      dayStreak: 1,
      points: 0,
      dailyGoalCount: 3,
      dailyCompletedCount: 0,
      darkThemeEnabled: false,
      notificationsEnabled: true,
      examRemindersEnabled: true,
      newMaterialsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student cannot self-assign admin role
  await assertFails(
    bobDb.collection("users").doc(STUDENT_B).set({
      userId: STUDENT_B,
      displayName: "Bob",
      email: "bob@example.com",
      selectedClass: 7,
      role: "admin",
      subscriptionTier: "free",
      avgScore: 0,
      dayStreak: 1,
      points: 0,
      dailyGoalCount: 3,
      dailyCompletedCount: 0,
      darkThemeEnabled: false,
      notificationsEnabled: true,
      examRemindersEnabled: true,
      newMaterialsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student cannot update role or subscriptionTier or ghost fields
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_A).update({
      role: "admin",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_A).update({
      subscriptionTier: "premium",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_A).update({
      isVerified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).update({
      selectedClass: 8,
      points: 50,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(STUDENT_A).get());
  await assertFails(bobDb.collection("users").doc(STUDENT_A).update({ displayName: "Hacked", updatedAt: pastDate(1000) }));
});

test("User subcollections (bookmarks, quiz_attempts, reading_progress, downloads) are strictly owner-isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_B).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).collection("bookmarks").doc("bm_1").set({
      materialId: "mat_1",
      title: "The Selfish Giant Notes",
      subjectName: "English",
      classGrade: 7,
      materialType: "short_notes",
      subtitle: "Short Notes",
      accentKey: "blue",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).collection("quiz_attempts").doc("att_1").set({
      quizId: "quiz_1",
      quizTitle: "Past Tense Quiz",
      subjectName: "English",
      classGrade: 7,
      mode: "practice",
      totalQuestions: 5,
      correctAnswers: 4,
      scorePercent: 80,
      durationSeconds: 95,
      pointsEarned: 40,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(STUDENT_A).collection("bookmarks").get());
  await assertFails(bobDb.collection("users").doc(STUDENT_A).collection("bookmarks").get());
  await assertFails(bobDb.collection("users").doc(STUDENT_A).collection("quiz_attempts").get());
});

test("Email spoofing defense & curriculum access rules", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_A, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate();

  // Unverified admin email cannot create curriculum
  await assertFails(
    spoofedAdminDb.collection("study_materials").doc("mat_1").set({
      classGrade: 7,
      subjectId: "sub_eng_7",
      subjectName: "English",
      chapterId: "chap_3",
      chapterTitle: "Chapter 3: The Selfish Giant",
      title: "Short Notes",
      description: "Key takeaways and vocabulary",
      materialType: "short_notes",
      contentMarkdown: "# Summary\nThe Giant learned to share his garden.",
      fileSizeLabel: "1.2 MB",
      tags: ["grammar", "literature"],
      important: true,
      premiumOnly: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified admin creates published + draft study materials
  await assertSucceeds(
    realAdminDb.collection("study_materials").doc("mat_pub").set({
      classGrade: 7,
      subjectId: "sub_eng_7",
      subjectName: "English",
      chapterId: "chap_3",
      chapterTitle: "Chapter 3: The Selfish Giant",
      title: "Short Notes",
      description: "Key takeaways",
      materialType: "short_notes",
      contentMarkdown: "# Summary\nThe Giant learned to share.",
      fileSizeLabel: "1.2 MB",
      tags: ["literature"],
      important: true,
      premiumOnly: false,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("study_materials").doc("mat_draft").set({
      classGrade: 7,
      subjectId: "sub_eng_7",
      subjectName: "English",
      chapterId: "chap_3",
      chapterTitle: "Chapter 3: The Selfish Giant",
      title: "Draft Notes",
      description: "Unpublished",
      materialType: "short_notes",
      contentMarkdown: "Draft content",
      fileSizeLabel: "0.5 MB",
      tags: [],
      important: false,
      premiumOnly: false,
      published: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Student aligned query (published == true) succeeds; unfiltered list fails; draft get fails; student write fails
  await assertSucceeds(studentDb.collection("study_materials").where("published", "==", true).get());
  await assertFails(studentDb.collection("study_materials").get());
  await assertFails(studentDb.collection("study_materials").doc("mat_draft").get());
  await assertFails(studentDb.collection("study_materials").doc("mat_pub").update({ title: "Hacked", updatedAt: pastDate(1000) }));

  // Verified admin can list all including drafts
  await assertSucceeds(realAdminDb.collection("study_materials").get());
});

test("Admin can manage app_settings with Ad Management fields while students can only read published settings", async () => {
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_A, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    realAdminDb.collection("app_settings").doc("global").set({
      premiumMonthlyPrice: "$4.99/mo",
      premiumYearlyPrice: "$39.99/yr",
      adsEnabled: true,
      adFamilySafeMode: true,
      adPlacementHome: true,
      adPlacementMaterials: true,
      adPlacementQuizzes: false,
      adPlacementPapers: true,
      adBannerTitle: "StudyHub Plus • Family-Safe Learning",
      adBannerSubtitle: "Unlock unlimited offline packs, advanced analytics & ad-free mode.",
      adCtaLabel: "Upgrade",
      adNetworkProvider: "StudyHub Direct (Family-Safe)",
      maintenanceMode: false,
      supportEmail: "support@studyhub.edu",
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("app_settings").doc("global").update({
      adsEnabled: false,
      adPlacementQuizzes: true,
      adBannerTitle: "Ad-Free Scholarship Week",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(studentDb.collection("app_settings").where("published", "==", true).get());
  await assertFails(
    studentDb.collection("app_settings").doc("global").update({
      adsEnabled: false,
      updatedAt: pastDate(1000),
    })
  );
});

test("Admin can update exam_papers and manage PDF/image paper_attachments while students can only read published attachments", async () => {
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_A, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastDate();

  await assertSucceeds(
    realAdminDb.collection("exam_papers").doc("paper_2026_sci").set({
      classGrade: 7,
      subjectId: "sub_science_7",
      subjectName: "Science",
      title: "Class 7 Science Model Paper",
      paperCategory: "model_paper",
      year: 2026,
      fileSizeLabel: "PDF • 320 KB",
      contentText: "Section A: MCQs",
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("exam_papers").doc("paper_2026_sci").update({
      title: "Class 7 Science Updated Paper",
      fileSizeLabel: "PDF + 1 Image • 440 KB",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    realAdminDb.collection("paper_attachments").doc("att_pdf_1_c0").set({
      parentId: "paper_2026_sci",
      groupId: "att_pdf_1",
      kind: "pdf",
      fileName: "science_2026.pdf",
      mimeType: "application/pdf",
      sizeBytes: 320000,
      chunkIndex: 0,
      totalChunks: 1,
      sortOrder: 0,
      published: true,
      dataBase64: "JVBERi0xLjQK",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    realAdminDb.collection("paper_attachments").doc("att_img_draft_c0").set({
      parentId: "paper_2026_sci",
      groupId: "att_img_draft",
      kind: "image",
      fileName: "page_1.jpg",
      mimeType: "image/jpeg",
      sizeBytes: 95000,
      chunkIndex: 0,
      totalChunks: 1,
      sortOrder: 1,
      published: false,
      dataBase64: "/9j/4AAQ",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    studentDb.collection("paper_attachments")
      .where("parentId", "==", "paper_2026_sci")
      .where("published", "==", true)
      .get()
  );
  await assertFails(
    studentDb.collection("paper_attachments").where("parentId", "==", "paper_2026_sci").get()
  );
  await assertFails(
    studentDb.collection("paper_attachments").doc("att_img_draft_c0").get()
  );
  await assertFails(
    studentDb.collection("paper_attachments").doc("att_pdf_1_c0").delete()
  );

  await assertSucceeds(realAdminDb.collection("paper_attachments").doc("att_img_draft_c0").delete());
});


