import "./env.js";
import NodeCache from "node-cache";
import { Redis } from "@upstash/redis";

const defaultTtl = Number(process.env.CACHE_TTL_TOP_HEADLINES_SECONDS || 900);
const memoryCache = new NodeCache({
  stdTTL: defaultTtl,
  checkperiod: 120,
  useClones: false
});

const redisUrl = process.env.UPSTASH_REDIS_REST_URL;
const redisToken = process.env.UPSTASH_REDIS_REST_TOKEN;
const redis = redisUrl && redisToken ? new Redis({ url: redisUrl, token: redisToken }) : null;

function ttlOrDefault(ttlSeconds) {
  const ttl = Number(ttlSeconds || defaultTtl);
  return Number.isFinite(ttl) && ttl > 0 ? ttl : defaultTtl;
}

export function isRedisConfigured() {
  return Boolean(redis);
}

export async function getCache(key) {
  if (!key) return undefined;

  if (redis) {
    try {
      const value = await redis.get(key);
      if (value !== null && value !== undefined) return value;
    } catch (error) {
      console.warn(`Redis get failed for ${key}: ${error.message}`);
    }
  }

  return memoryCache.get(key);
}

export async function setCache(key, value, ttlSeconds) {
  if (!key || value === undefined) return;
  const ttl = ttlOrDefault(ttlSeconds);

  memoryCache.set(key, value, ttl);

  if (redis) {
    try {
      await redis.set(key, value, { ex: ttl });
    } catch (error) {
      console.warn(`Redis set failed for ${key}: ${error.message}`);
    }
  }
}

export async function deleteCache(key) {
  if (!key) return;
  memoryCache.del(key);

  if (redis) {
    try {
      await redis.del(key);
    } catch (error) {
      console.warn(`Redis delete failed for ${key}: ${error.message}`);
    }
  }
}

export async function incrementRateLimit(key, ttlSeconds) {
  if (!key) return { count: 0, ttl: ttlOrDefault(ttlSeconds), redis: false };
  const ttl = ttlOrDefault(ttlSeconds);

  if (redis) {
    try {
      const count = await redis.incr(key);
      if (count === 1) await redis.expire(key, ttl);
      return { count, ttl, redis: true };
    } catch (error) {
      console.warn(`Redis rate limit failed for ${key}: ${error.message}`);
    }
  }

  const current = Number(memoryCache.get(key) || 0) + 1;
  memoryCache.set(key, current, ttl);
  return { count: current, ttl, redis: false };
}

export function cacheStats() {
  return {
    redisConfigured: Boolean(redis),
    memory: memoryCache.getStats()
  };
}
