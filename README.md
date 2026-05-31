# News-Reader-Android

Ứng dụng đọc báo Android viết bằng Kotlin, XML Layout/ViewBinding, Jetpack Compose cho một số màn hình xác thực/quản trị và mô hình MVVM. Project hỗ trợ Home feed theo chuyên mục, tìm kiếm, chi tiết bài viết, bookmark, download/offline, lịch sử đọc, Reader Mode/WebView, cá nhân hóa, dark mode, OTP, Google/Firebase sign-in, push notification và backend proxy Node.js.

## 1. Chạy theo cấu hình backend đã chuẩn bị

Bản này đã đặt `local.properties` để Android Emulator gọi backend local:

```properties
NEWS_BASE_URL=http://10.0.2.2:8080/
NEWS_API_KEY=
BACKEND_APP_TOKEN=change_this_demo_token
NEWS_API_DAILY_LIMIT=90
```

Chạy backend trước:

```bash
cd backend
npm install
npm run dev
```

Kiểm tra backend:

```bash
curl http://localhost:8080/health
```

Sau đó mở project bằng Android Studio, Sync Gradle và Run app trên Emulator.

## 2. Chạy trên điện thoại thật hoặc deploy

Điện thoại thật không gọi được `10.0.2.2`. Hãy deploy backend lên Railway/Render/Cloud Run hoặc dùng HTTPS tunnel, rồi đổi `NEWS_BASE_URL` trong `local.properties`:

```properties
NEWS_BASE_URL=https://your-backend-url/
NEWS_API_KEY=
BACKEND_APP_TOKEN=change_this_demo_token
```

## 3. Backend đã hỗ trợ

- NewsAPI proxy: `/v2/top-headlines`, `/v2/everything`, `/v2/top-headlines/sources`.
- Reader Mode: `/api/reader`.
- Related articles: `/api/related`.
- OTP email: SendGrid.
- OTP SMS: Twilio Verify.
- Google/Firebase token verification: Firebase Admin SDK.
- Push notification: FCM topic/device.
- Device token store: PostgreSQL.
- Cache và OTP cooldown: Upstash Redis REST.

## 4. Build

```bash
./gradlew :app:assembleDebug
```

Nếu dùng Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

## 5. Bảo mật

Không public các file/cấu hình thật:

```text
local.properties
backend/.env
backend/firebase-service-account.json
app/google-services.json
```

Xem thêm: `RUN_WITH_FULL_SERVICES.md`, `RUN_WITH_YOUR_CONFIG.md`, `backend/README.md`.
