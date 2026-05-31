import crypto from "crypto";

export function createId(prefix = "id") {
  return `${prefix}_${crypto.randomUUID()}`;
}

export function hashValue(value = "") {
  return crypto.createHash("sha256").update(String(value)).digest("hex");
}
