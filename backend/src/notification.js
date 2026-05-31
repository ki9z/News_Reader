import "./env.js";
import admin from "firebase-admin";
import fs from "fs";
import { query, isPostgresAvailable } from "./db/postgres.js";
import { createId, hashValue } from "./utils/id.js";

let initialized = false;

export function initializeFirebaseAdmin() {
  if (initialized || admin.apps.length) {
    initialized = true;
    return true;
  }

  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (!serviceAccountPath || !fs.existsSync(serviceAccountPath)) {
    return false;
  }

  const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  initialized = true;
  return true;
}

async function logNotification({ title, body, articleUrl, topic, token, sentCount = 0, failedCount = 0, providerMessageId }) {
  if (!isPostgresAvailable()) return;
  try {
    await query(
      `INSERT INTO notification_logs(id, title, body, article_url, topic, token_hash, sent_count, failed_count, provider_message_id)
       VALUES($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [
        createId("notif"),
        title || "News update",
        body || "",
        articleUrl || "",
        topic || null,
        token ? hashValue(token) : null,
        sentCount,
        failedCount,
        providerMessageId || null
      ]
    );
  } catch (error) {
    console.warn(`Notification log failed: ${error.message}`);
  }
}

export async function sendToTopic(topic, article = {}) {
  if (!initializeFirebaseAdmin()) {
    const error = new Error("Firebase Admin is not configured");
    error.status = 503;
    error.code = "firebaseNotConfigured";
    throw error;
  }

  const title = article.title || "Breaking news";
  const body = article.description || article.source?.name || "Tap to read more";
  const articleUrl = article.url || "";

  const message = {
    topic,
    notification: { title, body },
    data: {
      title,
      body,
      articleUrl,
      imageUrl: article.urlToImage || article.imageUrl || "",
      source: article.source?.name || article.source || ""
    }
  };

  const messageId = await admin.messaging().send(message);
  await logNotification({ title, body, articleUrl, topic, sentCount: 1, providerMessageId: messageId });
  return messageId;
}

export async function sendToDevice(token, article = {}) {
  if (!initializeFirebaseAdmin()) {
    const error = new Error("Firebase Admin is not configured");
    error.status = 503;
    error.code = "firebaseNotConfigured";
    throw error;
  }

  const title = article.title || "News update";
  const body = article.description || "Tap to read more";
  const articleUrl = article.url || "";

  const messageId = await admin.messaging().send({
    token,
    notification: { title, body },
    data: {
      title,
      body,
      articleUrl,
      imageUrl: article.urlToImage || article.imageUrl || ""
    }
  });

  await logNotification({ title, body, articleUrl, token, sentCount: 1, providerMessageId: messageId });
  return messageId;
}

export async function verifyFirebaseIdToken(idToken) {
  if (!initializeFirebaseAdmin()) {
    const error = new Error("Firebase Admin is not configured");
    error.status = 503;
    error.code = "firebaseNotConfigured";
    throw error;
  }
  return admin.auth().verifyIdToken(idToken);
}
