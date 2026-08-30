# Release Checklist

## نسخه و سازگاری به‌روزرسانی

1. `versionCode` افزایش یافته و `versionName` با انتشار هماهنگ باشد.
2. `VERSION.txt` و `update.json` دقیقاً همان نسخه را اعلام کنند.
3. `applicationId=ir.asteam.namedic` تغییر نکند.
4. برای Release فقط همان keystore خصوصی اصلی استفاده شود؛ ساخت کلید جدید ممنوع است.
5. نصب نسخه جدید روی نسخه قبلی بدون حذف برنامه روی دستگاه واقعی بررسی شود تا favorites/profile/settings حفظ شوند.

## داده

6. `python3 tools/validate_data.py`
7. `python3 tools/validate_history.py`
8. `python3 tools/validate_history_timeline.py`
9. pipeline همگام‌سازی نام‌ها اجرا و دادهٔ توسعه‌یافته دوباره validate شود.
10. هیچ معنی، ریشه یا فرهنگ نام از دادهٔ شخصیت تاریخی استنتاج نشود.
11. لینک منبع رکوردهای تاریخی HTTPS و شناسه‌ها در همه content packها یکتا باشند.

## کد و رابط

12. `gradle :app:testDebugUnitTest`
13. `gradle :app:lintDebug`
14. `gradle :app:assembleDebug`
15. Home، Drawer، Settings، Search، Girls/Boys، Discover، Favorites و مقایسه بررسی شوند.
16. پیشنهادگر پیشرفته: جنسیت، حرف اول، کلیدواژه، فرهنگ، فیلتر اطلاعات‌دار و surname preview بررسی شود.
17. History Hub: شخصیت‌ها، Timeline و Quiz بررسی شوند.
18. Detail: favorite، share، report data و related names بررسی شوند.
19. Back از صفحات داخلی به مسیر درست برگردد و خروج ناخواسته رخ ندهد.
20. پروفایل و انتخاب تصویر، Settings و Share with friends در Drawer بررسی شوند.
21. About نباید package/application ID را به کاربر نمایش دهد.

## Update checker و شبکه

22. بررسی نسخه جدید با اتصال اینترنت کار کند.
23. قطع اینترنت نباید قابلیت‌های آفلاین برنامه را متوقف کند.
24. Update dialog فقط وقتی `latestVersionCode > BuildConfig.VERSION_CODE` نمایش داده شود.

## Artifact

25. APK Debug نهایی، source ZIP، `BUILD_INFO.txt` و `SHA256SUMS.txt` از همان CI موفق تحویل داده شوند.
26. اگر keystore اصلی در محیط Build موجود نیست، خروجی را صریحاً Debug/آزمایشی اعلام کن و Release جعلی نساز.
27. اگر keystore اصلی موجود است، `:app:lintRelease :app:assembleRelease` اجرا و امضای APK و SHA-256 گواهی verify شود.

## محدودیت محیط فعلی

GitHub Actions صحت data validation، unit test، lint و compile/build را تأیید می‌کند. تست تعاملی واقعی فقط زمانی «انجام‌شده» محسوب می‌شود که APK روی emulator یا دستگاه Android واقعی اجرا شده باشد؛ صرف موفقیت CI معادل UI/device test نیست.
