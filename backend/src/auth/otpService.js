import "../env.js";
import crypto from "crypto";
import Twilio from "twilio";
import sendgrid from "@sendgrid/mail";
import { getCache, setCache, deleteCache, incrementRateLimit } from "../cache.js";
import { query, isPostgresAvailable } from "../db/postgres.js";
import { createId, hashValue } from "../utils/id.js";

const EMAIL_CHANNEL = "email";
const SMS_CHANNEL = "sms";
const OTP_TTL_SECONDS = Number(process.env.OTP_TTL_SECONDS || 300);
const OTP_MAX_ATTEMPTS = Number(process.env.OTP_MAX_ATTEMPTS || 5);
const OTP_RESEND_COOLDOWN_SECONDS = Number(process.env.OTP_RESEND_COOLDOWN_SECONDS || 60);

function normalizeChannel(channel) {
  const normalized = String(channel || EMAIL_CHANNEL).toLowerCase();
  if (["phone", "sms"].includes(normalized)) return SMS_CHANNEL;
  return EMAIL_CHANNEL;
}

function normalizeIdentifier(identifier, channel) {
  const value = String(identifier || "").trim();
  if (!value) {
    const error = new Error("Missing email or phone number");
    error.status = 400;
    error.code = "missingOtpIdentifier";
    throw error;
  }

  if (channel === EMAIL_CHANNEL && !/^\S+@\S+\.\S+$/.test(value)) {
    const error = new Error("Invalid email address");
    error.status = 400;
    error.code = "invalidEmail";
    throw error;
  }

  if (channel === SMS_CHANNEL && !/^\+?[0-9]{8,15}$/.test(value.replace(/[\s-]/g, ""))) {
    const error = new Error("Invalid phone number. Use E.164 format such as +84901234567.");
    error.status = 400;
    error.code = "invalidPhone";
    throw error;
  }

  return channel === SMS_CHANNEL ? value.replace(/[\s-]/g, "") : value.toLowerCase();
}

function otpKey(channel, identifier) {
  return `otp:${channel}:${hashValue(identifier)}`;
}

function cooldownKey(channel, identifier) {
  return `otp-cooldown:${channel}:${hashValue(identifier)}`;
}

function attemptsKey(channel, identifier) {
  return `otp-attempts:${channel}:${hashValue(identifier)}`;
}

function hashOtp(code, identifier) {
  return crypto
    .createHmac("sha256", process.env.OTP_SECRET || "dev-only-otp-secret")
    .update(`${identifier}:${code}`)
    .digest("hex");
}

function generateOtpCode() {
  return crypto.randomInt(100000, 999999).toString();
}

async function createOtpSession({ identifier, channel, provider, providerSessionId, otpHash }) {
  const expiresAt = new Date(Date.now() + OTP_TTL_SECONDS * 1000).toISOString();
  if (!isPostgresAvailable()) return null;

  try {
    const id = createId("otp");
    await query(
      `INSERT INTO otp_sessions(id, identifier, channel, otp_hash, provider, provider_session_id, expires_at)
       VALUES($1, $2, $3, $4, $5, $6, $7)`,
      [id, identifier, channel, otpHash || null, provider, providerSessionId || null, expiresAt]
    );
    return id;
  } catch (error) {
    console.warn(`OTP session log failed: ${error.message}`);
    return null;
  }
}

async function markOtpVerified({ identifier, channel }) {
  if (!isPostgresAvailable()) return;
  try {
    await query(
      `UPDATE otp_sessions
       SET verified_at = NOW()
       WHERE id = (
         SELECT id FROM otp_sessions
         WHERE identifier = $1 AND channel = $2 AND verified_at IS NULL
         ORDER BY created_at DESC
         LIMIT 1
       )`,
      [identifier, channel]
    );
  } catch (error) {
    console.warn(`OTP verified log failed: ${error.message}`);
  }
}

function getTwilioClient() {
  if (!process.env.TWILIO_ACCOUNT_SID || !process.env.TWILIO_AUTH_TOKEN || !process.env.TWILIO_VERIFY_SERVICE_SID) {
    const error = new Error("Twilio Verify is not configured");
    error.status = 503;
    error.code = "twilioNotConfigured";
    throw error;
  }
  return Twilio(process.env.TWILIO_ACCOUNT_SID, process.env.TWILIO_AUTH_TOKEN);
}

async function sendSmsOtp(identifier) {
  const client = getTwilioClient();
  const verification = await client.verify.v2
    .services(process.env.TWILIO_VERIFY_SERVICE_SID)
    .verifications.create({ to: identifier, channel: "sms" });

  await createOtpSession({
    identifier,
    channel: SMS_CHANNEL,
    provider: "twilio-verify",
    providerSessionId: verification.sid
  });

  return { provider: "twilio-verify", status: verification.status };
}

async function verifySmsOtp(identifier, code) {
  const client = getTwilioClient();
  const result = await client.verify.v2
    .services(process.env.TWILIO_VERIFY_SERVICE_SID)
    .verificationChecks.create({ to: identifier, code });

  if (result.status !== "approved") {
    const error = new Error("OTP is incorrect or expired");
    error.status = 400;
    error.code = "invalidOtp";
    throw error;
  }

  await markOtpVerified({ identifier, channel: SMS_CHANNEL });
  return { verified: true, provider: "twilio-verify" };
}

async function sendEmailOtp(identifier) {
  if (!process.env.SENDGRID_API_KEY || !process.env.SENDGRID_FROM_EMAIL) {
    const error = new Error("SendGrid is not configured");
    error.status = 503;
    error.code = "sendgridNotConfigured";
    throw error;
  }

  const code = generateOtpCode();
  const otpHash = hashOtp(code, identifier);
  await setCache(otpKey(EMAIL_CHANNEL, identifier), { otpHash, createdAt: Date.now() }, OTP_TTL_SECONDS);
  await deleteCache(attemptsKey(EMAIL_CHANNEL, identifier));
  await createOtpSession({
    identifier,
    channel: EMAIL_CHANNEL,
    provider: "sendgrid",
    otpHash
  });

  sendgrid.setApiKey(process.env.SENDGRID_API_KEY);
  await sendgrid.send({
    to: identifier,
    from: process.env.SENDGRID_FROM_EMAIL,
    subject: "News Reader OTP verification code",
    text: `Your News Reader verification code is ${code}. It expires in ${Math.floor(OTP_TTL_SECONDS / 60)} minutes.`,
    html: `<p>Your News Reader verification code is:</p><h2>${code}</h2><p>This code expires in ${Math.floor(OTP_TTL_SECONDS / 60)} minutes.</p>`
  });

  return { provider: "sendgrid", status: "sent" };
}

async function verifyEmailOtp(identifier, code) {
  const attempts = await incrementRateLimit(attemptsKey(EMAIL_CHANNEL, identifier), OTP_TTL_SECONDS);
  if (attempts.count > OTP_MAX_ATTEMPTS) {
    const error = new Error("Too many OTP attempts. Please request a new code.");
    error.status = 429;
    error.code = "tooManyOtpAttempts";
    throw error;
  }

  const stored = await getCache(otpKey(EMAIL_CHANNEL, identifier));
  if (!stored?.otpHash || stored.otpHash !== hashOtp(code, identifier)) {
    const error = new Error("OTP is incorrect or expired");
    error.status = 400;
    error.code = "invalidOtp";
    throw error;
  }

  await deleteCache(otpKey(EMAIL_CHANNEL, identifier));
  await deleteCache(attemptsKey(EMAIL_CHANNEL, identifier));
  await markOtpVerified({ identifier, channel: EMAIL_CHANNEL });
  return { verified: true, provider: "sendgrid" };
}

export async function requestOtp({ identifier, channel = EMAIL_CHANNEL } = {}) {
  const normalizedChannel = normalizeChannel(channel);
  const normalizedIdentifier = normalizeIdentifier(identifier, normalizedChannel);

  const cooldown = await getCache(cooldownKey(normalizedChannel, normalizedIdentifier));
  if (cooldown) {
    const error = new Error("Please wait before requesting another OTP");
    error.status = 429;
    error.code = "otpCooldown";
    throw error;
  }
  await setCache(cooldownKey(normalizedChannel, normalizedIdentifier), true, OTP_RESEND_COOLDOWN_SECONDS);

  const result = normalizedChannel === SMS_CHANNEL
    ? await sendSmsOtp(normalizedIdentifier)
    : await sendEmailOtp(normalizedIdentifier);

  return {
    status: "ok",
    channel: normalizedChannel,
    identifierMasked: maskIdentifier(normalizedIdentifier, normalizedChannel),
    expiresInSeconds: OTP_TTL_SECONDS,
    ...result
  };
}

export async function verifyOtp({ identifier, channel = EMAIL_CHANNEL, code } = {}) {
  const normalizedChannel = normalizeChannel(channel);
  const normalizedIdentifier = normalizeIdentifier(identifier, normalizedChannel);
  const normalizedCode = String(code || "").trim();

  if (!/^\d{6}$/.test(normalizedCode)) {
    const error = new Error("OTP must be a 6 digit code");
    error.status = 400;
    error.code = "invalidOtpFormat";
    throw error;
  }

  const result = normalizedChannel === SMS_CHANNEL
    ? await verifySmsOtp(normalizedIdentifier, normalizedCode)
    : await verifyEmailOtp(normalizedIdentifier, normalizedCode);

  return {
    status: "ok",
    channel: normalizedChannel,
    identifierMasked: maskIdentifier(normalizedIdentifier, normalizedChannel),
    ...result
  };
}

export function maskIdentifier(identifier, channel) {
  if (channel === EMAIL_CHANNEL) {
    const [name, domain] = identifier.split("@");
    const safeName = name.length <= 2 ? `${name[0] || "*"}*` : `${name.slice(0, 2)}***`;
    return `${safeName}@${domain}`;
  }
  return `${identifier.slice(0, 3)}***${identifier.slice(-2)}`;
}
