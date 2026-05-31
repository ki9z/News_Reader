import "./env.js";
import axios from "axios";
import { getCache, setCache } from "./cache.js";

const NEWS_API_BASE_URL = process.env.NEWS_API_BASE_URL || "https://newsapi.org";
const DEFAULT_PAGE_SIZE = Number(process.env.DEFAULT_PAGE_SIZE || 50);
const DEFAULT_COUNTRY = process.env.DEFAULT_COUNTRY || "us";

const newsApi = axios.create({
  baseURL: NEWS_API_BASE_URL,
  timeout: 15000,
  headers: {
    "X-Api-Key": process.env.NEWS_API_KEY || ""
  }
});

function ensureApiKey() {
  if (!process.env.NEWS_API_KEY) {
    const error = new Error("NEWS_API_KEY is not configured on the backend");
    error.status = 500;
    error.code = "missingBackendNewsApiKey";
    throw error;
  }
}

function cleanQuery(query = {}) {
  const cleaned = { ...query };
  delete cleaned.apiKey;
  delete cleaned["api-key"];

  const pageSize = Number(cleaned.pageSize || DEFAULT_PAGE_SIZE);
  cleaned.pageSize = Math.min(Math.max(Number.isFinite(pageSize) ? pageSize : DEFAULT_PAGE_SIZE, 1), 100);

  const page = Number(cleaned.page || 1);
  cleaned.page = Math.max(Number.isFinite(page) ? page : 1, 1);

  Object.keys(cleaned).forEach((key) => {
    if (cleaned[key] === undefined || cleaned[key] === null || cleaned[key] === "") {
      delete cleaned[key];
    }
  });

  return cleaned;
}

function stableQueryString(query) {
  return Object.keys(query)
    .sort()
    .map((key) => `${key}=${String(query[key])}`)
    .join("&");
}

function cacheKey(prefix, query) {
  return `${prefix}:${stableQueryString(cleanQuery(query))}`;
}

function normalizeNewsApiError(error) {
  const status = error.response?.status || error.status || 500;
  const data = error.response?.data;
  const normalized = new Error(data?.message || error.message || "News API request failed");
  normalized.status = status;
  normalized.code = data?.code || error.code || "newsApiError";
  normalized.retryAfter = error.response?.headers?.["retry-after"];
  return normalized;
}

async function fetchWithCache(prefix, path, query, ttlSeconds) {
  ensureApiKey();

  const params = cleanQuery(query);
  const key = cacheKey(prefix, params);
  const cached = await getCache(key);
  if (cached) {
    return { ...cached, cached: true };
  }

  try {
    const response = await newsApi.get(path, { params });
    const body = response.data;
    await setCache(key, body, ttlSeconds);
    return { ...body, cached: false };
  } catch (error) {
    throw normalizeNewsApiError(error);
  }
}

export function getTopHeadlines(query = {}) {
  const params = cleanQuery(query);

  // NewsAPI does not allow sources to be mixed with country or category.
  // Sanitize here so both Android and manual backend calls stay valid.
  if (params.sources) {
    delete params.country;
    delete params.category;
  }

  if (!params.country && !params.sources) {
    params.country = DEFAULT_COUNTRY;
  }

  return fetchWithCache(
    "top-headlines",
    "/v2/top-headlines",
    params,
    Number(process.env.CACHE_TTL_TOP_HEADLINES_SECONDS || 900)
  );
}

export function searchEverything(query = {}) {
  const params = cleanQuery(query);
  if (!params.q) {
    const error = new Error("Missing search query");
    error.status = 400;
    error.code = "missingQuery";
    throw error;
  }

  return fetchWithCache(
    "everything",
    "/v2/everything",
    params,
    Number(process.env.CACHE_TTL_EVERYTHING_SECONDS || 3600)
  );
}

export function getSources(query = {}) {
  return fetchWithCache(
    "sources",
    "/v2/top-headlines/sources",
    cleanQuery(query),
    Number(process.env.CACHE_TTL_SOURCES_SECONDS || 86400)
  );
}

export function buildRelatedQuery({ title = "", description = "", source = "" } = {}) {
  const raw = `${title} ${description} ${source}`
    .replace(/[|:()\[\]{}]/g, " ")
    .split(/\s+/)
    .map((word) => word.trim())
    .filter((word) => word.length >= 4)
    .filter((word) => !/^(with|from|this|that|have|will|after|before|about|news|says|said)$/i.test(word));

  return [...new Set(raw)].slice(0, 6).join(" ");
}
