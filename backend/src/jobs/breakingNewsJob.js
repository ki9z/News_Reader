import "../env.js";
import cron from "node-cron";
import { getTopHeadlines } from "../newsApi.js";
import { sendToTopic } from "../notification.js";

const sentUrls = new Set();

function isBreaking(article) {
  const text = `${article.title || ""} ${article.description || ""}`.toLowerCase();
  return /breaking|urgent|live|just in|developing|cảnh báo|khẩn|nóng/.test(text);
}

export function startBreakingNewsJob() {
  if (String(process.env.ENABLE_BREAKING_NEWS_JOB || "false").toLowerCase() !== "true") {
    return false;
  }

  const schedule = process.env.BREAKING_NEWS_CRON || "*/15 * * * *";
  const topic = process.env.BREAKING_NEWS_TOPIC || "breaking-news";

  cron.schedule(schedule, async () => {
    try {
      const result = await getTopHeadlines({ country: process.env.DEFAULT_COUNTRY || "us", pageSize: 20 });
      const articles = result.articles || [];

      for (const article of articles) {
        if (!article.url || sentUrls.has(article.url) || !isBreaking(article)) continue;
        await sendToTopic(topic, article);
        sentUrls.add(article.url);
      }
    } catch (error) {
      console.error("Breaking news job failed:", error.message);
    }
  });

  return true;
}
