# NewsAPI request handling

Bản này đã gom việc gọi NewsAPI vào một lớp bảo vệ chung để tránh vượt quota và giảm lỗi do gọi lặp.

## Cấu hình

Thêm vào `local.properties` ở thư mục gốc project:

```properties
NEWS_API_KEY=your_real_key_here
NEWS_BASE_URL=https://newsapi.org/
NEWS_API_DAILY_LIMIT=90
```

`NEWS_API_DAILY_LIMIT` là ngưỡng mềm trong app. Với key miễn phí nên để thấp hơn giới hạn thật một chút để còn vùng an toàn. Nếu dùng gói trả phí có quota cao hơn thì có thể tăng giá trị này.

## Luật đang áp dụng

- API key được gửi bằng header `X-Api-Key`, không đưa vào query string.
- Log debug không in API key; interceptor đã redact header nhạy cảm.
- Các request giống nhau đang chạy đồng thời được de-duplicate, chỉ gọi mạng một lần.
- App tự giãn nhịp giữa các request để tránh spam khi người dùng search/refresh nhanh.
- App đếm quota theo ngày UTC trong SharedPreferences.
- Khi gặp HTTP 429, app tạm backoff 15 phút.
- Khi gặp lỗi server 5xx, app tạm backoff 1 phút.
- Khi timeout/mất mạng, app tạm backoff ngắn và ưu tiên cache/offline.

## Cache TTL

- Top headlines: dùng cache mới trong 15 phút.
- Search: dùng cache mới trong 60 phút.
- Local news: dùng cache trong 3 giờ.
- Khi API lỗi/backoff/quota: cho phép dùng cache fallback trong 24 giờ.
- Khi thiếu API key: cho phép dùng cache cũ tối đa 7 ngày rồi fallback sang dữ liệu offline seed.
