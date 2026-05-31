# News Reader Backend

Backend này đứng giữa Android app và NewsAPI để giấu `NEWS_API_KEY`, cache dữ liệu, giảm quota, trích xuất nội dung Reader Mode, lưu device token, gửi OTP email/SMS và hỗ trợ Firebase push notification.

## 1. Chạy local

```bash
cd backend
npm install
npm run dev
```

Test nhanh:

```bash
curl http://localhost:8080/health
curl -H "X-App-Token: change_this_demo_token" "http://localhost:8080/v2/top-headlines?country=us&pageSize=10"
curl -H "X-App-Token: change_this_demo_token" "http://localhost:8080/v2/everything?q=technology&pageSize=10"
```

## 2. Kết nối Android Emulator với backend local

Trong `local.properties`:

```properties
NEWS_BASE_URL=http://10.0.2.2:8080/
NEWS_API_KEY=
BACKEND_APP_TOKEN=change_this_demo_token
NEWS_API_DAILY_LIMIT=90
FIREBASE_WEB_CLIENT_ID=your_firebase_web_client_id.apps.googleusercontent.com
```

`10.0.2.2` là địa chỉ để Android Emulator gọi về `localhost` của máy tính. Project đã mở cleartext riêng cho `10.0.2.2` trong `network_security_config.xml` để tiện chạy đồ án local.

Nếu dùng điện thoại thật, hãy deploy backend hoặc dùng HTTPS tunnel rồi đổi `NEWS_BASE_URL` sang URL đó.

## 3. Endpoint tương thích NewsAPI

```text
GET /v2/top-headlines
GET /v2/everything
GET /v2/top-headlines/sources
```

Backend tự gắn `NEWS_API_KEY` khi gọi NewsAPI thật và tự cache kết quả bằng Upstash Redis nếu đã cấu hình.

## 4. Endpoint bổ sung

```text
GET  /health
GET  /api/reader?url=https://...
GET  /api/related?q=...
POST /api/auth/otp/request
POST /api/auth/otp/verify
GET  /auth/send-otp?email=...
GET  /auth/verify-otp?email=...&otp=...
POST /api/auth/firebase
POST /api/devices/register
POST /api/devices/topics
GET  /api/devices
POST /api/notifications/test
```

## 5. OTP email và SMS

Gửi OTP email:

```bash
curl -X POST http://localhost:8080/api/auth/otp/request \
  -H "Content-Type: application/json" \
  -H "X-App-Token: change_this_demo_token" \
  -d '{"identifier":"user@example.com","channel":"email"}'
```

Xác thực OTP email:

```bash
curl -X POST http://localhost:8080/api/auth/otp/verify \
  -H "Content-Type: application/json" \
  -H "X-App-Token: change_this_demo_token" \
  -d '{"identifier":"user@example.com","channel":"email","code":"123456"}'
```

Gửi OTP SMS cần số điện thoại dạng E.164, ví dụ `+84901234567`.

## 6. Firebase push notification

File service account đặt tại:

```text
backend/firebase-service-account.json
```

Gửi test notification:

```bash
curl -X POST "http://localhost:8080/api/notifications/test" \
  -H "Content-Type: application/json" \
  -H "X-App-Token: change_this_demo_token" \
  -d '{
    "topic":"breaking-news",
    "article":{
      "title":"Test breaking news",
      "description":"Tap to open article",
      "url":"https://example.com/news/1",
      "urlToImage":""
    }
  }'
```

## 7. PostgreSQL và Redis

Backend tự tạo các bảng sau khi khởi động nếu `DATABASE_URL` hợp lệ:

```text
users, devices, device_topics, otp_sessions, notification_logs
```

Upstash Redis REST được dùng cho cache tin tức, cache Reader Mode, OTP email, cooldown và giới hạn số lần nhập OTP.

Lưu ý: URL `postgres.railway.internal` chỉ dùng được khi backend chạy trong cùng Railway project. Khi chạy local, hãy dùng public DATABASE_URL của Railway hoặc deploy backend lên Railway.

## 8. Bảo mật

Không commit công khai:

```text
backend/.env
backend/firebase-service-account.json
app/google-services.json
local.properties
```
