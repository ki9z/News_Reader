import "./env.js";
import axios from "axios";
import * as cheerio from "cheerio";
import { getCache, setCache } from "./cache.js";

const MAX_HTML_CHARS = Number(process.env.READER_MAX_HTML_CHARS || 1_200_000);
const MAX_TEXT_CHARS = Number(process.env.READER_MAX_TEXT_CHARS || 50_000);

function normalizeText(text = "") {
  return text
    .replace(/\u00a0/g, " ")
    .replace(/[ \t\r\f\v]+/g, " ")
    .replace(/\n\s+/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function isBlockedHost(hostname) {
  const host = hostname.toLowerCase().replace(/^\[|\]$/g, "");

  if (host === "localhost" || host.endsWith(".localhost") || host === "0.0.0.0") {
    return true;
  }

  if (host === "::1" || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80")) {
    return true;
  }

  const ipv4 = host.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
  if (!ipv4) return false;

  const parts = ipv4.slice(1).map(Number);
  if (parts.some((part) => part < 0 || part > 255)) return true;

  const [a, b] = parts;
  return (
    a === 10 ||
    a === 127 ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 168)
  );
}

function assertSafeUrl(url) {
  let parsed;
  try {
    parsed = new URL(url);
  } catch {
    const error = new Error("Invalid URL");
    error.status = 400;
    error.code = "invalidUrl";
    throw error;
  }

  if (!["http:", "https:"].includes(parsed.protocol)) {
    const error = new Error("Unsupported URL protocol");
    error.status = 400;
    error.code = "unsupportedProtocol";
    throw error;
  }

  if (isBlockedHost(parsed.hostname)) {
    const error = new Error("Reader extraction does not allow local or private network URLs");
    error.status = 400;
    error.code = "blockedReaderUrl";
    throw error;
  }

  return parsed.toString();
}

function absolutizeUrl(value, baseUrl) {
  if (!value) return "";
  try {
    return new URL(value, baseUrl).toString();
  } catch {
    return value;
  }
}

function paragraphCandidates($) {
  const selectors = [
    "article p",
    "main p",
    "[role='main'] p",
    ".article p",
    ".article-content p",
    ".story p",
    ".story-body p",
    ".entry-content p",
    ".content p",
    "p"
  ];

  const paragraphs = [];
  for (const selector of selectors) {
    $(selector).each((_, el) => {
      const text = normalizeText($(el).text());
      if (text.length >= 45 && !/subscribe|advertisement|enable javascript|cookie/i.test(text)) {
        paragraphs.push(text);
      }
    });
    if (paragraphs.length >= 4) break;
  }

  return [...new Set(paragraphs)];
}

export async function extractReaderContent(articleUrl) {
  const safeUrl = assertSafeUrl(articleUrl);
  const cacheKey = `reader:${safeUrl}`;
  const cached = await getCache(cacheKey);
  if (cached) return { ...cached, cached: true };

  const response = await axios.get(safeUrl, {
    timeout: 15000,
    maxRedirects: 5,
    responseType: "text",
    maxContentLength: MAX_HTML_CHARS,
    headers: {
      "User-Agent": "Mozilla/5.0 NewsReaderBackend/1.0",
      "Accept": "text/html,application/xhtml+xml"
    },
    transformResponse: [(data) => String(data).slice(0, MAX_HTML_CHARS)]
  });

  const contentType = response.headers?.["content-type"] || "";
  if (contentType && !contentType.toLowerCase().includes("text/html")) {
    const error = new Error("URL does not return HTML content");
    error.status = 415;
    error.code = "unsupportedContentType";
    throw error;
  }

  const $ = cheerio.load(response.data);
  $("script, style, nav, footer, header, aside, iframe, noscript, form").remove();

  const title = normalizeText(
    $("meta[property='og:title']").attr("content") ||
    $("meta[name='twitter:title']").attr("content") ||
    $("h1").first().text() ||
    $("title").text()
  );

  const description = normalizeText(
    $("meta[property='og:description']").attr("content") ||
    $("meta[name='description']").attr("content") ||
    ""
  );

  const imageUrl = absolutizeUrl(
    $("meta[property='og:image']").attr("content") ||
    $("meta[name='twitter:image']").attr("content") ||
    $("article img").first().attr("src") ||
    "",
    safeUrl
  );

  const paragraphs = paragraphCandidates($);
  const content = normalizeText(paragraphs.join("\n\n")).slice(0, MAX_TEXT_CHARS);

  const result = {
    url: safeUrl,
    title,
    description,
    imageUrl,
    content,
    wordCount: content ? content.split(/\s+/).length : 0,
    extractedAt: new Date().toISOString()
  };

  await setCache(cacheKey, result, Number(process.env.CACHE_TTL_READER_SECONDS || 86400));
  return { ...result, cached: false };
}
