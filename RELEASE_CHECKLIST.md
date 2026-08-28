# Release Checklist

1. `versionCode` را افزایش بده و `versionName` را تغییر بده.
2. `update.json` را هماهنگ کن.
3. `applicationId=ir.asteam.namedic` را تغییر نده.
4. از همان keystore خصوصی استفاده کن و آن را وارد GitHub نکن.
5. `python3 tools/validate_data.py` را اجرا کن.
6. در صورت نیاز `python3 tools/sync_names.py` را اجرا کن.
7. `./gradlew :app:lintRelease :app:assembleRelease` را اجرا کن.
8. Back، Drawer، جستجو، فیلتر جنسیت، علاقه‌مندی و عکس پروفایل را تست کن.
9. نصب نسخه جدید روی نسخه قبلی را بدون حذف برنامه تست کن.
