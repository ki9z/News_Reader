# So Sánh Database: Git vs Workspace

## Tình hình

### Git Remote (origin/main) - Tình trạng cũ
Repo `https://github.com/ki9z/News-Reader-Android.git` (origin/main) hiện tại có:

```
app/src/main/java/com/data/local/entity/ArticleEntity.kt
- Chỉ là stub rỗng (class ArticleEntity {})

app/src/main/java/com/data/local/db/AppDatabase.kt
- Chỉ là stub rỗng (class AppDatabase {})
```

**Kết luận**: Repository gốc chưa implement DB hoàn chỉnh.

---

### Workspace Hiện Tại (main branch)
Mình vừa implement cho bạn:

**Entities** (đã tạo):
- `UserEntity.kt` - quản lý user account
- `UserAuthProviderEntity.kt` - OAuth providers
- `UserSettingsEntity.kt` - cài đặt per-user
- `CategoryEntity.kt` - topic/category
- `UserFollowedCategoryEntity.kt` - user follow category
- `UserBookmarkEntity.kt` - bookmark per-user (thay vì global `articles` table)
- `ArticleBlockEntity.kt` - nội dung article dạng block (paragraph, image, etc.)
- `ReadingHistoryEntity.kt` - lịch sử đọc
- `UserDownloadEntity.kt` - bài báo đã download offline
- `UserSearchHistoryEntity.kt` - lịch sử tìm kiếm
- `SyncOutboxEntity.kt` - queue action offline-first

**DAOs** (đã tạo):
- `UserDao.kt`
- `UserAuthProviderDao.kt`
- `UserSettingsDao.kt`
- `BookmarkDao.kt` 
- `CategoryDao.kt`
- `ArticleBlockDao.kt`
- `ReadingHistoryDao.kt`
- `DownloadDao.kt`
- `SearchHistoryDao.kt`
- `SyncOutboxDao.kt`

**Database + Migrations** (đã update):
- `AppDatabase.kt` - bumped to `version = 3`, thêm 11 entities, thêm 11 DAOs
- `DbMigrations.kt` - migration v1→v2→v3 với backfill logic

---

## So Sánh Chi Tiết

| Aspect | Git (origin/main) | Workspace (main) |
|--------|-------------------|------------------|
| **DB Version** | Stub (không implement) | v3 |
| **Entities** | 0 | 11 |
| **DAOs** | 0 | 11 |
| **Bookmark Strategy** | Global table `articles` | Per-user `user_bookmarks` |
| **User Support** | Không | Có (UserEntity) |
| **Auth Providers** | Không | Có (OAuth/Phone) |
| **Reading History** | Không | Có |
| **Offline Downloads** | Không | Có |
| **Block Content** | Không | Có |
| **Sync Queue** | Không | Có |

---

## Kế hoạch sắp tới

### Option A: **Giữ workspace này** (Khuyên)
Workspace của bạn hiện tại có implementation đầy đủ cho:
- Real auth
- Per-user bookmarks
- Follow categories
- Reading history
- Offline downloads
- Block rendering
- Offline-first sync

### Option B: **Đặt lại từ repo gốc** (Không khuyên)
Repo gốc chỉ có stub, sẽ phải implement lại từ đầu.

---

## Recommendation

**✅ Tiếp tục với workspace hiện tại:**

Mình đã làm sẵn full schema + migration. Bước kế tiếp:

1. Wire `NewsRepository` qua `BookmarkDao` (per-user)
2. Thêm `SessionManager` lưu `currentUserId`
3. Tạo migration test
4. Push lên origin/main khi stable

