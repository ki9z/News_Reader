import "./env.js";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import { cacheStats, isRedisConfigured } from "./cache.js";
import { getSources, getTopHeadlines, searchEverything, buildRelatedQuery } from "./newsApi.js";
import { extractReaderContent } from "./readerExtractor.js";
import { initializeFirebaseAdmin, sendToDevice, sendToTopic, verifyFirebaseIdToken } from "./notification.js";
import { registerDevice, updateDeviceTopics, listDevices } from "./storage/deviceStore.js";
import { startBreakingNewsJob } from "./jobs/breakingNewsJob.js";
import { initDatabase, isPostgresConfigured, isPostgresAvailable, disablePostgres, testDatabaseConnection, query, postgresStatus } from "./db/postgres.js";
import { requestOtp, verifyOtp } from "./auth/otpService.js";
import { createId } from "./utils/id.js";

const app = express();
const port = Number(process.env.PORT || 8080);

app.set("trust proxy", 1);
app.use(helmet());
app.use(cors({ origin: process.env.CORS_ORIGIN || "*" }));
app.use(express.json({ limit: "1mb" }));

app.use(rateLimit({
  windowMs: 60 * 1000,
  max: Number(process.env.REQUESTS_PER_MINUTE || 60),
  standardHeaders: true,
  legacyHeaders: false
}));

function requireAppToken(req, res, next) {
  const expected = process.env.APP_CLIENT_TOKEN;
  if (!expected) return next();

  const received = req.header("X-App-Token");
  if (received !== expected) {
    return res.status(401).json({
      status: "error",
      code: "invalidClientToken",
      message: "Invalid app token"
    });
  }

  next();
}

function sendError(res, error) {
  const status = error.status || 500;
  res.status(status).json({
    status: "error",
    code: error.code || "backendError",
    message: error.message || "Backend error"
  });
}

async function ensureDefaultUserFromFirebase(decodedToken) {
  if (!isPostgresAvailable()) return null;
  const userId = `firebase_${decodedToken.uid}`;
  const email = decodedToken.email || null;
  const name = decodedToken.name || decodedToken.email || "Firebase user";
  const avatarUrl = decodedToken.picture || null;

  await query(
    `INSERT INTO users(id, email, display_name, avatar_url, auth_provider, firebase_uid, updated_at)
     VALUES($1, $2, $3, $4, 'firebase', $5, NOW())
     ON CONFLICT(firebase_uid)
     DO UPDATE SET email = COALESCE(EXCLUDED.email, users.email), display_name = EXCLUDED.display_name,
                   avatar_url = EXCLUDED.avatar_url, updated_at = NOW()
     RETURNING id, email, display_name AS "displayName", avatar_url AS "avatarUrl", auth_provider AS "authProvider", firebase_uid AS "firebaseUid"`,
    [userId, email, name, avatarUrl, decodedToken.uid]
  );

  const result = await query(
    `SELECT id, email, display_name AS "displayName", avatar_url AS "avatarUrl", auth_provider AS "authProvider", firebase_uid AS "firebaseUid"
     FROM users WHERE firebase_uid = $1 LIMIT 1`,
    [decodedToken.uid]
  );
  return result.rows[0] || null;
}

app.get("/health", async (_req, res) => {
  res.json({
    status: "ok",
    service: "news-reader-backend",
    firebaseConfigured: initializeFirebaseAdmin(),
    postgresConfigured: isPostgresConfigured(),
    postgres: postgresStatus(),
    redisConfigured: isRedisConfigured(),
    database: await testDatabaseConnection(),
    cache: cacheStats()
  });
});

app.get("/v2/top-headlines", requireAppToken, async (req, res) => {
  try {
    res.json(await getTopHeadlines(req.query));
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/v2/everything", requireAppToken, async (req, res) => {
  try {
    res.json(await searchEverything(req.query));
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/v2/top-headlines/sources", requireAppToken, async (req, res) => {
  try {
    res.json(await getSources(req.query));
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/api/reader", requireAppToken, async (req, res) => {
  try {
    if (!req.query.url) {
      return res.status(400).json({ status: "error", code: "missingUrl", message: "Missing article URL" });
    }
    const article = await extractReaderContent(String(req.query.url));
    res.json({ status: "ok", article });
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/api/related", requireAppToken, async (req, res) => {
  try {
    const q = req.query.q || buildRelatedQuery(req.query);
    if (!q) {
      return res.status(400).json({ status: "error", code: "missingRelatedQuery", message: "Missing related article query" });
    }

    const data = await searchEverything({
      q,
      language: req.query.language,
      page: req.query.page || 1,
      pageSize: req.query.pageSize || 10,
      sortBy: req.query.sortBy || "relevancy"
    });
    res.json(data);
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/api/auth/otp/request", requireAppToken, async (req, res) => {
  try {
    res.json(await requestOtp(req.body || {}));
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/api/auth/otp/verify", requireAppToken, async (req, res) => {
  try {
    res.json(await verifyOtp(req.body || {}));
  } catch (error) {
    sendError(res, error);
  }
});

// Backward-compatible endpoints for older Android OTP code.
app.get("/auth/send-otp", requireAppToken, async (req, res) => {
  try {
    await requestOtp({ identifier: req.query.email, channel: "email" });
    res.type("text/plain").send("Success");
  } catch (error) {
    res.status(error.status || 500).type("text/plain").send(error.message || "Failed");
  }
});

app.get("/auth/verify-otp", requireAppToken, async (req, res) => {
  try {
    await verifyOtp({ identifier: req.query.email, channel: "email", code: req.query.otp });
    res.type("text/plain").send("Success");
  } catch (error) {
    res.status(error.status || 500).type("text/plain").send("Failed");
  }
});

app.post("/api/auth/firebase", requireAppToken, async (req, res) => {
  try {
    const token = req.header("Authorization")?.replace(/^Bearer\s+/i, "") || req.body?.idToken;
    if (!token) {
      return res.status(400).json({ status: "error", code: "missingFirebaseIdToken", message: "Missing Firebase ID token" });
    }
    const decoded = await verifyFirebaseIdToken(token);
    const user = await ensureDefaultUserFromFirebase(decoded);
    res.json({ status: "ok", user: user || { firebaseUid: decoded.uid, email: decoded.email || null } });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/api/devices/register", requireAppToken, async (req, res) => {
  try {
    res.json({ status: "ok", device: await registerDevice(req.body || {}) });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/api/devices/topics", requireAppToken, async (req, res) => {
  try {
    const { token, topics } = req.body || {};
    res.json({ status: "ok", device: await updateDeviceTopics(token, topics) });
  } catch (error) {
    sendError(res, error);
  }
});

app.get("/api/devices", requireAppToken, async (_req, res) => {
  try {
    res.json({ status: "ok", devices: await listDevices() });
  } catch (error) {
    sendError(res, error);
  }
});

app.post("/api/notifications/test", requireAppToken, async (req, res) => {
  try {
    const { token, topic = "breaking-news", article = {} } = req.body || {};
    const result = token ? await sendToDevice(token, article) : await sendToTopic(topic, article);
    res.json({ status: "ok", messageId: result });
  } catch (error) {
    sendError(res, error);
  }
});

async function start() {
  try {
    await initDatabase();
    console.log("Database initialization: done or skipped");
  } catch (error) {
    console.error("Database initialization failed:", error.message);
    disablePostgres(error.message);
  }

  const jobStarted = startBreakingNewsJob();
  app.listen(port, () => {
    console.log(`News Reader backend running on port ${port}`);
    console.log(`Breaking news job: ${jobStarted ? "enabled" : "disabled"}`);
  });
}

start();
