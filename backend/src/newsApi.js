import "./env.js";
import axios from "axios";
import { getCache, setCache } from "./cache.js";

const NEWS_API_BASE_URL = process.env.NEWS_API_BASE_URL || "https://newsapi.org";
const DEFAULT_PAGE_SIZE = Number(process.env.DEFAULT_PAGE_SIZE || 50);
const DEFAULT_COUNTRY = process.env.DEFAULT_COUNTRY || "us";

const FALLBACK_ARTICLES = [
  {
    source: { id: "vnexpress", name: "VnExpress" },
    author: "",
    title: "Cấm trẻ dùng mạng, được không?",
    description: "Bài viết phân tích cách cân bằng giữa bảo vệ trẻ em và quyền tiếp cận thông tin trong môi trường số.",
    url: "https://vnexpress.net/cam-tre-dung-mang-duoc-khong-4876543.html",
    urlToImage: "https://i2-vnexpress.vnecdn.net/2026/04/18/ManhDung-1776501471-2046-1776501497.jpg?w=1200&h=0&q=100&dpr=1&fit=crop&s=6Vpc8VK8R4edK6y-u7L2pg",
    publishedAt: "2026-04-18T08:39:00.000Z",
    content: "Trẻ em cần được bảo vệ khỏi xâm hại nhưng cũng cần được tiếp cận thông tin và học kỹ năng số an toàn."
  },
  {
    source: { id: "tuoitre", name: "Tuổi Trẻ" },
    author: "Phạm Tuấn",
    title: "Bí thư Hà Nội Trần Đức Thắng: Xử lý nghiêm vi phạm dọc kênh La Khê",
    description: "Hà Nội yêu cầu xử lý dứt điểm các vướng mắc hạ tầng và vi phạm hành lang kênh La Khê.",
    url: "https://tuoitre.vn/bi-thu-ha-noi-tran-duc-thang-xu-ly-nghiem-vi-pham-doc-kenh-la-khe-20260418.htm",
    urlToImage: "https://cdn2.tuoitre.vn/thumb_w/1200/471584752817336320/2026/4/18/thjyukm-1776510245359342712669-164-0-1473-2500-crop-17765105405761633889160.jpg",
    publishedAt: "2026-04-18T11:55:00.000Z",
    content: "Bí thư Thành ủy Hà Nội yêu cầu hoàn thành nền đường vành đai 1 trước 2-9 và xử lý các vi phạm dọc kênh La Khê."
  },
  {
    source: { id: "thanhnien", name: "Thanh Niên" },
    author: "Hà Khanh",
    title: "Hà Nội sắp vào 'cách mạng' hạn chế xe xăng, dầu",
    description: "Thủ đô thí điểm vùng phát thải thấp, thúc đẩy người dân chuyển sang giao thông xanh.",
    url: "https://thanhnien.vn/ha-noi-sap-vao-cach-mang-han-che-xe-xang-dau-185260418.htm",
    urlToImage: "https://images2.thanhnien.vn/zoom/1200_630/528068263637045248/2026/4/18/dji0884-17359738928681028667808-17765034659301674427793-42-0-1642-2560-crop-17765035956391375515056.jpg",
    publishedAt: "2026-04-18T09:36:00.000Z",
    content: "Hà Nội chuẩn bị thí điểm vùng phát thải thấp tại khu vực Vành đai 1 và mở rộng giao thông xanh."
  },
  {
    source: { id: "vietnamnet", name: "VietnamNet" },
    author: "",
    title: "Chuyển đổi số trong giáo dục: trường học cần làm gì ngay hôm nay?",
    description: "Các trường học được khuyến nghị triển khai kỹ năng số thực hành và an toàn mạng cho học sinh.",
    url: "https://vietnamnet.vn/chuyen-doi-so-trong-giao-duc-truong-hoc-can-lam-gi-ngay-hom-nay-20260418.html",
    urlToImage: "https://static.vnncdn.net/vnexpress/2026/04/18/edu-digital.jpg",
    publishedAt: "2026-04-18T13:20:00.000Z",
    content: "Giáo dục kỹ năng số cần được đưa vào trường học như một phần thực chất của an toàn trực tuyến."
  },
  {
    source: { id: "cafef", name: "CafeF" },
    author: "",
    title: "Doanh nghiệp nhỏ đẩy mạnh số hóa bán hàng, tối ưu chi phí vận hành",
    description: "Nhiều doanh nghiệp vừa và nhỏ đang chuyển sang công cụ số để tăng hiệu quả kinh doanh.",
    url: "https://cafef.vn/doanh-nghiep-nho-day-manh-so-hoa-ban-hang-20260418.chn",
    urlToImage: "https://cafefcdn.com/thumb_w/1200/2026/4/18/digital-business.jpg",
    publishedAt: "2026-04-18T15:10:00.000Z",
    content: "Số hóa giúp doanh nghiệp nhỏ giảm chi phí, cải thiện quy trình và mở rộng kênh bán hàng."
  },
  {
    source: { id: "dantri", name: "Dân Trí" },
    author: "",
    title: "Nhiều thành phố tăng tốc xây dựng hạ tầng giao thông công cộng",
    description: "Metro, xe buýt điện và bãi đỗ trung chuyển đang được ưu tiên tại các đô thị lớn.",
    url: "https://dantri.com.vn/nhieu-thanh-pho-tang-toc-xay-dung-ha-tang-giao-thong-cong-cong-20260418.htm",
    urlToImage: "https://cdnimg.dantri.com.vn/transport-20260418.jpg",
    publishedAt: "2026-04-18T17:05:00.000Z",
    content: "Giao thông công cộng là chìa khóa để các đô thị giảm khí thải và ùn tắc."
  }
];

const FALLBACK_SOURCES = [...new Map(FALLBACK_ARTICLES.map((article) => [article.source?.name, article.source])).values()]
  .filter(Boolean)
  .map((source) => ({
    id: source.id,
    name: source.name,
    description: `${source.name} - nguồn tin mẫu dự phòng khi backend chưa cấu hình NEWS_API_KEY.`,
    url: `https://example.com/${String(source.id || source.name).toLowerCase()}`,
    category: "general",
    language: "en",
    country: DEFAULT_COUNTRY
  }));

const newsApi = axios.create({
  baseURL: NEWS_API_BASE_URL,
  timeout: 15000,
  headers: {
    "X-Api-Key": process.env.NEWS_API_KEY || ""
  }
});

function hasApiKey() {
  return Boolean(process.env.NEWS_API_KEY && process.env.NEWS_API_KEY.trim());
}

function cloneFallback(value) {
  return JSON.parse(JSON.stringify(value));
}

function textIncludes(haystack, needle) {
  return String(haystack || "").toLowerCase().includes(String(needle || "").toLowerCase());
}

function fallbackArticles(query = {}) {
  const params = cleanQuery(query);
  const q = String(params.q || "").trim();

  let articles = cloneFallback(FALLBACK_ARTICLES);

  if (params.sources) {
    const wanted = String(params.sources)
      .split(",")
      .map((item) => item.trim().toLowerCase())
      .filter(Boolean);
    if (wanted.length) {
      articles = articles.filter((article) => wanted.some((value) => textIncludes(article.source?.id, value) || textIncludes(article.source?.name, value)));
    }
  }

  if (params.country) {
    // Keep the fallback data visible for any country request, but bias to a stable set.
    articles = articles.filter((article) => article.source?.name) || articles;
  }

  if (params.category) {
    articles = articles.filter((article) => textIncludes(article.title, params.category) || textIncludes(article.description, params.category) || textIncludes(article.content, params.category));
  }

  if (q) {
    const terms = q.split(/\s+/).filter(Boolean);
    articles = articles.filter((article) => {
      const haystack = `${article.title || ""} ${article.description || ""} ${article.content || ""} ${article.source?.name || ""}`.toLowerCase();
      return terms.every((term) => haystack.includes(term.toLowerCase()));
    });
  }

  const page = Number(params.page || 1);
  const pageSize = Number(params.pageSize || DEFAULT_PAGE_SIZE);
  const start = (Math.max(page, 1) - 1) * Math.min(Math.max(pageSize, 1), 100);
  const sliced = articles.slice(start, start + Math.min(Math.max(pageSize, 1), 100));

  return {
    status: "ok",
    totalResults: articles.length,
    articles: sliced,
    cached: false,
    fallback: true
  };
}

function fallbackSources(query = {}) {
  const params = cleanQuery(query);
  const sources = FALLBACK_SOURCES.filter((source) => {
    if (params.category && !textIncludes(source.category, params.category)) return false;
    if (params.country && !textIncludes(source.country, params.country)) return false;
    if (params.language && !textIncludes(source.language, params.language)) return false;
    return true;
  });

  return {
    status: "ok",
    sources,
    fallback: true
  };
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
  if (!hasApiKey()) {
    return fallbackArticles(query);
  }

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
    if (error.response?.status === 401 || error.response?.status === 403 || error.code === "ERR_BAD_REQUEST") {
      return fallbackArticles(query);
    }
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
  if (!hasApiKey()) {
    return fallbackSources(query);
  }

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
