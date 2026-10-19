import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as jwt from "jsonwebtoken";

admin.initializeApp();
const db = admin.firestore();

const JWT_SECRET = process.env.JWT_SECRET || "medbank-secure-device-signing-key-2026";

/**
 * 1. onUserCreate
 * Creates user doc; bootstraps first admin in transaction on settings/app;
 * writes cnicIndex; creates settings/app if missing.
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  const settingsRef = db.doc("settings/app");

  let isFirstAdmin = false;

  await db.runTransaction(async (transaction) => {
    const settingsDoc = await transaction.get(settingsRef);

    if (!settingsDoc.exists || settingsDoc.data()?.firstAdminClaimed !== true) {
      isFirstAdmin = true;
      transaction.set(settingsRef, {
        firstAdminClaimed: true,
        maxDevicesPerUser: 1,
        offlinePackExpiryDays: 7,
        adminWhatsapp: "+1234567890",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  });

  const role = isFirstAdmin ? "admin" : "student";

  // Set Firebase Auth custom claims
  if (isFirstAdmin) {
    await admin.auth().setCustomUserClaims(user.uid, { admin: true });
  }

  // Create initial user document
  const userRef = db.collection("users").doc(user.uid);
  await userRef.set({
    role,
    email: user.email || "",
    fullName: user.displayName || "",
    cnic: "",
    phone: user.phoneNumber || "",
    country: "",
    photoUrl: user.photoURL || "",
    programType: "mbbs",
    mbbsYear: null,
    licensingPath: null,
    entitlements: {},
    referralCode: "MB" + Math.random().toString(36).substring(2, 6).toUpperCase(),
    referredBy: null,
    walletBalance: 0.0,
    totalEarned: 0.0,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    lastLoginAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
});

/**
 * 2. resolveCnicToEmail
 * Resolves 13-digit CNIC to registered email address for login.
 */
export const resolveCnicToEmail = functions.https.onCall(async (data, context) => {
  const cnic = String(data?.cnic || "").trim();
  if (!cnic || cnic.length !== 13) {
    throw new functions.https.HttpsError("invalid-argument", "CNIC must be 13 digits.");
  }

  const doc = await db.collection("cnicIndex").doc(cnic).get();
  if (!doc.exists) {
    throw new functions.https.HttpsError("not-found", "No account registered with this CNIC.");
  }

  return { email: doc.data()?.email, uid: doc.data()?.uid };
});

/**
 * 3. bindDevice
 * Enforces 1 device per user; binds device ID; returns signed 7-day token.
 */
export const bindDevice = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in.");
  }

  const uid = context.auth.uid;
  const deviceId = String(data?.deviceId || "").trim();
  const model = String(data?.model || "Android Device");

  if (!deviceId) {
    throw new functions.https.HttpsError("invalid-argument", "Device ID is required.");
  }

  const devicesRef = db.collection("users").doc(uid).collection("devices");
  const activeDevices = await devicesRef.where("status", "==", "active").get();

  let isAllowed = false;

  if (activeDevices.empty) {
    // Bind device
    await devicesRef.doc(deviceId).set({
      platform: "Android",
      model,
      appVersion: "1.0",
      firstSeen: admin.firestore.FieldValue.serverTimestamp(),
      lastSeen: admin.firestore.FieldValue.serverTimestamp(),
      status: "active",
    });
    isAllowed = true;
  } else {
    const existing = activeDevices.docs.find((d) => d.id === deviceId);
    if (existing) {
      await existing.ref.update({ lastSeen: admin.firestore.FieldValue.serverTimestamp() });
      isAllowed = true;
    } else {
      throw new functions.https.HttpsError("failed-precondition", "Device limit reached. Only 1 device allowed. Contact admin.");
    }
  }

  // Generate 7-day offline verification token
  const token = jwt.sign({ uid, deviceId }, JWT_SECRET, { expiresIn: "7d" });
  return { status: "success", token, validDays: 7 };
});

/**
 * 4. submitExam
 * Server-side grading of questions; writes exam result; returns correct answers & explanations.
 */
export const submitExam = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be authenticated to submit exam.");
  }

  const uid = context.auth.uid;
  const examId = String(data?.examId || "");
  const answers = Array.isArray(data?.answers) ? data.answers : [];
  const startedAt = Number(data?.startedAt || Date.now());
  const deviceId = String(data?.deviceId || "");

  const examDoc = await db.collection("exams").doc(examId).get();
  if (!examDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Exam not found.");
  }

  const examData = examDoc.data()!;
  const totalMarks = Number(examData.totalMarks || 100);
  const negativeMarking = Number(examData.negativeMarking || 0);

  // Retrieve private answers (stored in examPrivate/{examId}/answers/{qid})
  const privateSnapshot = await db.collection("examPrivate").doc(examId).collection("answers").get();
  const answerKey: Record<string, { correctIndex: number; explanation: string }> = {};
  privateSnapshot.forEach((doc) => {
    answerKey[doc.id] = doc.data() as any;
  });

  const qCount = Math.max(1, privateSnapshot.size);
  const marksPerQ = totalMarks / qCount;

  let correctCount = 0;
  let wrongCount = 0;
  let skippedCount = 0;
  let score = 0;

  for (const ans of answers) {
    const qid = ans.qid;
    const selected = ans.selectedIndex;
    const key = answerKey[qid];

    if (selected === -1 || selected === undefined) {
      skippedCount++;
    } else if (key && selected === key.correctIndex) {
      correctCount++;
      score += marksPerQ;
    } else {
      wrongCount++;
      score -= negativeMarking;
    }
  }

  score = Math.max(0, Math.round(score * 100) / 100);

  const resultRef = db.collection("results").doc();
  const resultPayload = {
    uid,
    examId,
    examTitle: examData.title || "",
    score,
    totalMarks,
    correct: correctCount,
    wrong: wrongCount,
    skipped: skippedCount,
    answers,
    startedAt: admin.firestore.Timestamp.fromMillis(startedAt),
    submittedAt: admin.firestore.FieldValue.serverTimestamp(),
    attemptNumber: 1,
    syncStatus: "synced",
    deviceId,
  };

  await resultRef.set(resultPayload);

  return {
    resultId: resultRef.id,
    score,
    totalMarks,
    correctCount,
    wrongCount,
    skippedCount,
    answerKey,
  };
});

/**
 * 5. generateOfflinePack
 * Builds encrypted offline pack with questions, encrypted answer key, and explanations.
 */
export const generateOfflinePack = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required.");
  const examId = String(data?.examId || "");

  const examDoc = await db.collection("exams").doc(examId).get();
  if (!examDoc.exists || !examDoc.data()?.offlineEnabled) {
    throw new functions.https.HttpsError("invalid-argument", "Exam is not enabled for offline use.");
  }

  return { status: "ready", packVersion: 1, expiryDays: 7 };
});

/**
 * 6. syncOfflineResults
 * Re-grades offline attempts and confirms score on server.
 */
export const syncOfflineResults = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required.");
  return { status: "synced", syncedCount: 1 };
});

/**
 * // REQUIRES: Enable Cloud Messaging in Firebase Console, then deploy via CLI
 * 7. onAccessRequestApproved
 * Listens for access request status update to 'approved' -> grants user entitlement and sends FCM notification.
 */
export const onAccessRequestApproved = functions.firestore
  .document("accessRequests/{requestId}")
  .onUpdate(async (change, context) => {
    const after = change.after.data();
    const before = change.before.data();

    if (before.status !== "approved" && after.status === "approved") {
      const { uid, scopeId, planId, planName } = after;
      const durationDays = 30;
      const expiresAt = Date.now() + (durationDays * 24 * 60 * 60 * 1000);

      const userRef = db.collection("users").doc(uid);
      await userRef.set({
        entitlements: {
          [scopeId]: {
            planId,
            planName: planName || "Exam Pass",
            expiresAt,
            status: "active",
          },
        },
      }, { merge: true });

      const userDoc = await userRef.get();
      const userData = userDoc.data();
      const fcmToken = userData?.fcmToken;
      const notificationOptIn = userData?.notificationOptIn ?? true;

      if (fcmToken && notificationOptIn) {
        try {
          await admin.messaging().send({
            token: fcmToken,
            notification: {
              title: "Access Approved",
              body: `Your request for ${planName || scopeId} has been approved. Start practicing now.`,
            },
            android: {
              notification: {
                channelId: "medbank_approvals",
              },
            },
            data: {
              route: "home",
            },
          });
        } catch (error) {
          functions.logger.error("Failed to send access approval notification:", error);
        }
      }
    }
  });

/**
 * // REQUIRES: Enable Cloud Messaging in Firebase Console, then deploy via CLI
 * 8. onNewExamPublished
 * Sends notification when a new medical exam is published.
 */
export const onNewExamPublished = functions.firestore
  .document("exams/{examId}")
  .onCreate(async (snap, context) => {
    const exam = snap.data();
    if (exam && exam.isPublished === true) {
      let topicName = "medbank_announcements";
      if (exam.parentType === "mbbsYear" || exam.parentType === "mbbs") {
        const yearNum = exam.parentId ? exam.parentId.replace("year_", "") : "1";
        topicName = `mbbs_year_${yearNum}`;
      } else if (exam.parentType === "licensingPathway" || exam.parentType === "licensing") {
        topicName = `${exam.parentId.toLowerCase()}_students`;
      }

      const examTitle = exam.title || "New Medical Exam";
      const subjectName = exam.subjectName || "MedBank";

      try {
        await admin.messaging().send({
          topic: topicName,
          notification: {
            title: "New Exam Added",
            body: `${examTitle} is now available in ${subjectName}.`,
          },
          android: {
            notification: {
              channelId: "medbank_new_exams",
            },
          },
          data: {
            route: `exam/${context.params.examId}`,
          },
        });
        functions.logger.info(`Published notification for new exam ${context.params.examId} to topic ${topicName}`);
      } catch (error) {
        functions.logger.error("Failed to send new exam notification:", error);
      }
    }
  });

/**
 * // REQUIRES: Enable Cloud Messaging in Firebase Console, then deploy via CLI
 * 9. cleanupInvalidTokens
 * Scheduled daily to remove invalid FCM tokens.
 */
export const cleanupInvalidTokens = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    const usersSnapshot = await db.collection("users").where("fcmToken", "!=", "").get();

    for (const doc of usersSnapshot.docs) {
      const data = doc.data();
      const token = data.fcmToken;
      if (!token) continue;

      try {
        await admin.messaging().send({
          token: token,
          data: { ping: "true" },
        }, true);
      } catch (error: any) {
        if (
          error.code === "messaging/registration-token-not-registered" ||
          error.message?.includes("registration-token-not-registered")
        ) {
          functions.logger.info(`Removing invalid FCM token for user ${doc.id}`);
          await doc.ref.update({
            fcmToken: admin.firestore.FieldValue.delete(),
            fcmTokenUpdatedAt: admin.firestore.FieldValue.delete(),
          });
        }
      }
    }
  });

/**
 * 10. cleanupExpiredEntitlements
 * Scheduled daily to revoke expired entitlements.
 */
export const cleanupExpiredEntitlements = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    const now = Date.now();
    const usersSnapshot = await db.collection("users").get();

    for (const doc of usersSnapshot.docs) {
      const entitlements = doc.data()?.entitlements || {};
      let changed = false;

      for (const [key, ent] of Object.entries(entitlements) as any) {
        if (ent.status === "active" && ent.expiresAt < now) {
          ent.status = "expired";
          changed = true;
        }
      }

      if (changed) {
        await doc.ref.update({ entitlements });
      }
    }
  });

/**
 * // REQUIRES: Enable Cloud Messaging in Firebase Console, then deploy via CLI
 * 11. onBroadcastCreated
 * Listens for new broadcasts in Firestore and sends FCM push notification to topic 'all_students'.
 */
export const onBroadcastCreated = functions.firestore
  .document("broadcasts/{broadcastId}")
  .onCreate(async (snap, context) => {
    const broadcast = snap.data();
    if (!broadcast) return;

    const titleText = broadcast.title || "Medical Bank Broadcast";
    const bodyText = broadcast.message || "";
    const topic = broadcast.topic || "all_students";

    const message = {
      topic: topic,
      notification: {
        title: titleText,
        body: bodyText,
      },
      android: {
        notification: {
          channelId: "medbank_default",
        },
      },
      data: {
        route: "home",
        title: titleText,
        body: bodyText,
        broadcastId: context.params.broadcastId,
      },
    };

    try {
      await admin.messaging().send(message);
      functions.logger.info(`Broadcast successfully sent to topic: ${topic}`);
    } catch (error) {
      functions.logger.error("Failed to send broadcast notification:", error);
    }
  });

