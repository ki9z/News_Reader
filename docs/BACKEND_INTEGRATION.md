# Backend integration for Android News Reader

## Mục tiêu

Backend được thêm vào project để app Android không cần giữ `NEWS_API_KEY` trong APK. Backend giữ API key trong `.env`, gọi NewsAPI bằng header `X-Api-Key`, cache kết quả, trích xuất Reader Mode, gửi OTP email/SMS, lưu device token và gửi Firebase Cloud Messaging.

## Kiến trúc

```text
Android app
   |
   | X-App-Token
   v
News Reader Backend
   |-- NewsAPI proxy: /v2/top-headlines, /v2/everything, /v2/top-headlines/sources
   |-- Reader extractor: /api/reader
   |-- OTP email: SendGrid + Redis cooldown/session
   |-- OTP SMS: Twilio Verify
   |-- Push notification: Firebase Admin SDK
   |-- Device store: PostgreSQL
   |-- Cache/quota: Upstash Redis REST + memory fallback
   v
PostgreSQL / Redis / NewsAPI / Firebase / Twilio / SendGrid
```

## Android config hiện tại

Bản này dùng backend local qua Android Emulator:

```properties
NEWS_BASE_URL=http://10.0.2.2:8080/
NEWS_API_KEY=
BACKEND_APP_TOKEN=change_this_demo_token
NEWS_API_DAILY_LIMIT=90
FIREBASE_WEB_CLIENT_ID=your_firebase_web_client_id.apps.googleusercontent.com
```

Khi `NEWS_BASE_URL` không chứa `newsapi.org`, app tự coi là backend proxy mode. Retrofit vẫn gọi endpoint NewsAPI-compatible như `/v2/top-headlines`, nhưng request đi qua backend.

## Endpoint backend

- `GET /health`: kiểm tra Firebase, PostgreSQL, Redis, cache.
- `GET /v2/top-headlines`: proxy tin mới.
- `GET /v2/everything`: proxy tìm kiếm/pagination.
- `GET /v2/top-headlines/sources`: proxy nguồn tin.
- `GET /api/reader?url=`: trích xuất bài viết gốc cho Reader Mode/Offline.
- `GET /api/related`: tìm bài liên quan.
- `POST /api/auth/otp/request`: gửi OTP email/SMS.
- `POST /api/auth/otp/verify`: xác thực OTP email/SMS.
- `GET /auth/send-otp`, `GET /auth/verify-otp`: endpoint tương thích code cũ.
- `POST /api/auth/firebase`: xác thực Firebase ID token từ Google Sign-In.
- `POST /api/devices/register`: lưu FCM token vào PostgreSQL.
- `POST /api/devices/topics`: cập nhật topic theo device.
- `GET /api/devices`: kiểm tra danh sách device đã đăng ký.
- `POST /api/notifications/test`: gửi FCM test theo token hoặc topic.

## Bảo mật và giới hạn

- NewsAPI key chỉ ở backend `.env`.
- Android gửi `X-App-Token` để chặn client không hợp lệ mức cơ bản.
- Backend dùng `helmet`, `express-rate-limit`, timeout khi gọi NewsAPI và cache TTL.
- Reader endpoint chặn URL localhost/private IP để giảm rủi ro SSRF.
- OTP email có TTL, cooldown và giới hạn số lần thử; dữ liệu OTP được hash trước khi lưu.
- Device token và notification log được lưu PostgreSQL; token trong notification log chỉ lưu dạng hash.

## Lưu ý deploy

- `postgres.railway.internal` chỉ hoạt động khi backend deploy trong cùng Railway project.
- Nếu chạy backend local, cần dùng public DATABASE_URL của Railway nếu muốn test PostgreSQL thật; nếu URL internal không kết nối được, backend vẫn chạy và fallback memory cho device store.
- Không public các file thật: `backend/.env`, `backend/firebase-service-account.json`, `app/google-services.json`, `local.properties`.
