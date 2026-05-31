import { query, isPostgresAvailable } from "../db/postgres.js";
import { createId } from "../utils/id.js";

const devices = new Map();

function sanitizeTopics(topics = []) {
  return [...new Set((Array.isArray(topics) ? topics : [])
    .map((topic) => String(topic).trim())
    .filter(Boolean)
    .slice(0, 30))];
}

function sanitizeDevicePayload({ token, topics = [], platform = "android", appVersion = null, language = null, userId = null } = {}) {
  if (!token || typeof token !== "string") {
    const error = new Error("Missing device token");
    error.status = 400;
    error.code = "missingDeviceToken";
    throw error;
  }

  return {
    token: token.trim(),
    topics: sanitizeTopics(topics),
    platform: String(platform || "android").trim(),
    appVersion: appVersion ? String(appVersion).trim() : null,
    language: language ? String(language).trim() : null,
    userId: userId ? String(userId).trim() : null
  };
}

async function upsertTopics(deviceId, topics) {
  for (const topic of topics) {
    await query(
      `INSERT INTO device_topics(id, device_id, topic, enabled, updated_at)
       VALUES($1, $2, $3, TRUE, NOW())
       ON CONFLICT(device_id, topic)
       DO UPDATE SET enabled = TRUE, updated_at = NOW()`,
      [createId("topic"), deviceId, topic]
    );
  }
}

export async function registerDevice(payload = {}) {
  const data = sanitizeDevicePayload(payload);

  if (isPostgresAvailable()) {
    const id = createId("dev");
    const result = await query(
      `INSERT INTO devices(id, user_id, fcm_token, platform, app_version, language, is_active, last_seen_at, updated_at)
       VALUES($1, $2, $3, $4, $5, $6, TRUE, NOW(), NOW())
       ON CONFLICT(fcm_token)
       DO UPDATE SET
         user_id = COALESCE(EXCLUDED.user_id, devices.user_id),
         platform = EXCLUDED.platform,
         app_version = EXCLUDED.app_version,
         language = EXCLUDED.language,
         is_active = TRUE,
         last_seen_at = NOW(),
         updated_at = NOW()
       RETURNING id, fcm_token AS token, platform, app_version AS "appVersion", language, last_seen_at AS "lastSeenAt"`,
      [id, data.userId, data.token, data.platform, data.appVersion, data.language]
    );

    const device = result.rows[0];
    if (data.topics.length) await upsertTopics(device.id, data.topics);
    return { ...device, topics: data.topics };
  }

  const value = {
    token: data.token,
    topics: data.topics,
    platform: data.platform,
    appVersion: data.appVersion,
    language: data.language,
    updatedAt: new Date().toISOString()
  };
  devices.set(data.token, value);
  return value;
}

export async function updateDeviceTopics(token, topics = []) {
  const normalizedToken = String(token || "").trim();
  if (!normalizedToken) {
    const error = new Error("Missing device token");
    error.status = 400;
    error.code = "missingDeviceToken";
    throw error;
  }
  const normalizedTopics = sanitizeTopics(topics);

  if (isPostgresAvailable()) {
    const result = await query(
      `SELECT id, fcm_token AS token, platform, app_version AS "appVersion", language
       FROM devices WHERE fcm_token = $1 LIMIT 1`,
      [normalizedToken]
    );

    let device = result.rows[0];
    if (!device) {
      device = await registerDevice({ token: normalizedToken, topics: normalizedTopics });
      return device;
    }

    await query(`UPDATE device_topics SET enabled = FALSE, updated_at = NOW() WHERE device_id = $1`, [device.id]);
    await upsertTopics(device.id, normalizedTopics);
    return { ...device, topics: normalizedTopics };
  }

  const current = devices.get(normalizedToken) || { token: normalizedToken, platform: "android" };
  const value = {
    ...current,
    topics: normalizedTopics,
    updatedAt: new Date().toISOString()
  };
  devices.set(normalizedToken, value);
  return value;
}

export async function listDevices() {
  if (isPostgresAvailable()) {
    const result = await query(
      `SELECT d.id, d.fcm_token AS token, d.platform, d.app_version AS "appVersion", d.language,
              d.is_active AS "isActive", d.last_seen_at AS "lastSeenAt",
              COALESCE(array_remove(array_agg(CASE WHEN dt.enabled THEN dt.topic END), NULL), '{}') AS topics
       FROM devices d
       LEFT JOIN device_topics dt ON dt.device_id = d.id
       GROUP BY d.id
       ORDER BY d.last_seen_at DESC
       LIMIT 200`
    );
    return result.rows;
  }

  return [...devices.values()];
}
