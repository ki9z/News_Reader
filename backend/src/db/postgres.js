import "../env.js";
import pg from "pg";

const { Pool } = pg;
let pool;
let initialized = false;
let postgresDisabledReason = null;

function shouldUseSsl() {
  const value = String(process.env.DATABASE_SSL || "").toLowerCase();
  if (["true", "1", "yes", "require"].includes(value)) return true;
  if (["false", "0", "no", "disable"].includes(value)) return false;
  return /supabase\.co|amazonaws\.com|render\.com/i.test(process.env.DATABASE_URL || "");
}

export function isPostgresConfigured() {
  return Boolean(process.env.DATABASE_URL);
}

export function isPostgresAvailable() {
  return isPostgresConfigured() && !postgresDisabledReason;
}

export function disablePostgres(reason = "Database connection failed") {
  postgresDisabledReason = reason;
}

export function postgresStatus() {
  return {
    configured: isPostgresConfigured(),
    enabled: isPostgresAvailable(),
    disabledReason: postgresDisabledReason
  };
}

export function getPool() {
  if (!isPostgresConfigured()) return null;
  if (!pool) {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL,
      max: Number(process.env.PG_POOL_MAX || 5),
      idleTimeoutMillis: 30_000,
      connectionTimeoutMillis: 10_000,
      ssl: shouldUseSsl() ? { rejectUnauthorized: false } : undefined
    });
  }
  return pool;
}

export async function query(text, params = []) {
  const currentPool = getPool();
  if (!currentPool || postgresDisabledReason) {
    const error = new Error(postgresDisabledReason || "DATABASE_URL is not configured");
    error.status = 503;
    error.code = "databaseNotConfigured";
    throw error;
  }
  return currentPool.query(text, params);
}

export async function testDatabaseConnection() {
  if (!isPostgresConfigured()) return { configured: false, connected: false, enabled: false };
  if (postgresDisabledReason) return { configured: true, connected: false, enabled: false, error: postgresDisabledReason };
  try {
    const result = await query("SELECT NOW() AS now");
    return { configured: true, connected: true, enabled: true, now: result.rows[0]?.now };
  } catch (error) {
    return { configured: true, connected: false, enabled: true, error: error.message };
  }
}

export async function initDatabase() {
  if (!isPostgresAvailable() || initialized) return false;

  await query(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE,
      phone TEXT UNIQUE,
      display_name TEXT,
      avatar_url TEXT,
      auth_provider TEXT NOT NULL DEFAULT 'local',
      firebase_uid TEXT UNIQUE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS devices (
      id TEXT PRIMARY KEY,
      user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
      fcm_token TEXT NOT NULL UNIQUE,
      platform TEXT NOT NULL DEFAULT 'android',
      app_version TEXT,
      language TEXT,
      is_active BOOLEAN NOT NULL DEFAULT TRUE,
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS device_topics (
      id TEXT PRIMARY KEY,
      device_id TEXT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
      topic TEXT NOT NULL,
      enabled BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE(device_id, topic)
    );

    CREATE TABLE IF NOT EXISTS otp_sessions (
      id TEXT PRIMARY KEY,
      identifier TEXT NOT NULL,
      channel TEXT NOT NULL,
      otp_hash TEXT,
      provider TEXT NOT NULL,
      provider_session_id TEXT,
      expires_at TIMESTAMPTZ NOT NULL,
      attempt_count INT NOT NULL DEFAULT 0,
      verified_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_otp_identifier_channel_created
      ON otp_sessions(identifier, channel, created_at DESC);

    CREATE TABLE IF NOT EXISTS notification_logs (
      id TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      body TEXT,
      article_url TEXT,
      topic TEXT,
      token_hash TEXT,
      sent_count INT NOT NULL DEFAULT 0,
      failed_count INT NOT NULL DEFAULT 0,
      provider_message_id TEXT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);

  initialized = true;
  return true;
}
