# Project Info

- Repository: `App-NameDic`
- User-facing name: `نام‌نامه ایران`
- Application ID / namespace: `ir.asteam.namedic`
- Version: `1.1.0` (`versionCode=4`)
- Kotlin + Jetpack Compose + Material 3
- AGP 9.3.0 / Gradle 9.5.0 / JDK 17
- minSdk: 24
- targetSdk: 36
- compileSdk: 36

معماری برنامه offline-first است: `MainActivity`، رابط Compose در `ui/App.kt`، مخزن داده در `data/NameRepository.kt`، بررسی نسخه در `data/UpdateChecker.kt` و مدل‌ها در `model/Models.kt`.

از نسخه 1.1.0 لایه داده از چند منبع دارای مجوز روشن ساخته می‌شود. `tools/sync_names.py` فهرست‌های باز را ادغام می‌کند، `tools/sync_wikidata_cultures.py` طبقه‌بندی ساختاری را اضافه می‌کند و `tools/sync_wiktionary_names.py` داده‌های ساختاری Wiktionary/Kaikki مثل تلفظ و romanization را enrich می‌کند.

علاقه‌مندی و URI عکس پروفایل در SharedPreferences نگهداری می‌شوند. applicationId و signing ثابت می‌مانند تا آپدیت روی نسخه قبلی نصب شود و داده کاربر حفظ شود.
