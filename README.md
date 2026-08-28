# App-NameDic — نام‌نامه ایران

«نام‌نامه ایران» یک برنامه Android آفلاین و قابل توسعه برای شناخت نام‌ها، معنی/توضیح، ریشه زبانی، فرهنگ یا زبان استفاده و میراث تاریخی ایران است.

## نسخه 1.2.0

- Kotlin + Jetpack Compose + Material 3
- minSdk 24 / targetSdk 36 / compileSdk 36
- AGP 9.3.0 / Gradle 9.5.0 / Java 17
- corpus چندمنبعی به‌جای یک فهرست نام
- dedupe و نرمال‌سازی خودکار نام‌های تکراری
- رأی‌گیری چندمنبعی جنسیت و ثبت تعارض به‌جای حدس
- چند شکل لاتین برای هر نام در صورت وجود
- enrichment ساختاری از Wikidata
- enrichment تلفظ/romanization/ریشه صریح از Wiktionary/Kaikki
- منبع MIT `mohammadhejazirad/persian-names` برای افزایش پوشش نام/جنسیت
- لایه واژگانی CC BY-SA از `Maani/Dehkhoda-Lexicon`
- تفکیک صریح «معنی شخص‌نام» از «هم‌معنی/متضاد واژه فارسی هم‌نوشت با نام»
- ادغام فیلدبه‌فیلد curated/base برای حفظ تلفظ، provenance و enrichment چندمنبعی
- نگهداری `sourceIds` و provenance هر رکورد
- گزارش پوشش واقعی در `data_build_info.json`
- جستجو در نام، معنی مستقیم، داده واژگانی، ریشه و نوشتار لاتین
- صفحه «همه نام‌ها»
- 16 دسته زبانی/فرهنگی + دسته خنثی «فهرست عمومی ایران»
- قهرمانان، اساطیر و جانوران فرهنگی
- علاقه‌مندی‌های آفلاین
- Drawer مشترک پروژه با پروفایل و انتخاب عکس با یک لمس
- Back صحیح از صفحات داخلی
- About بدون نمایش package name
- بررسی اختیاری نسخه جدید؛ استفاده روزمره فرهنگ‌نامه همچنان offline-first است

## اصل حیاتی داده‌ها

سه مفهوم در نسخه 1.2.0 مستقل هستند:

1. `meaning` = معنی/توضیح مستقیم خود شخص‌نام با منبع مستقیم.
2. `origin` = ریشه زبانی/تاریخی منبع‌دار.
3. `lexicalMeaningFa` / `lexicalAntonymsFa` = اطلاعات واژه فارسی هم‌نوشت با نام.

هم‌نوشت بودن یک واژه با نام، رایج بودن نام در یک فرهنگ، و ریشه زبانی آن سه ادعای متفاوت‌اند. برنامه هیچ‌کدام را از روی دیگری حدس نمی‌زند.

## pipeline داده

منابع خودکار دارای مجوز روشن شامل چند دیتاست نام ایرانی/فارسی، Wikidata، Wiktionary/Kaikki و Maani/Dehkhoda-Lexicon هستند. سامانه تعاملی نام ثبت احوال به‌عنوان مرجع رسمی بررسی می‌شود اما تا زمانی که API عمومی و شرایط برداشت ماشینی روشن نباشد bulk-scrape نمی‌شود.

```bash
python3 tools/sync_names.py
python3 tools/sync_mohammadhejazirad_names.py
python3 tools/restore_enriched_orphans.py /path/to/previous.json
python3 tools/sync_dehkhoda_lexical.py
python3 tools/sync_wikidata_cultures.py
python3 tools/sync_wiktionary_names.py
python3 tools/build_data_report.py
python3 tools/validate_data.py
./gradlew :app:assembleDebug
```

جزئیات دقیق منبع، مجوز و فیلدهای قابل استفاده در `DATA_SOURCES.md` و `THIRD_PARTY_NOTICES.md` ثبت شده است.

## فایل‌های داده

- `app/src/main/assets/names_base.json`: corpus ادغام‌شده و deduplicate شده.
- `app/src/main/assets/curated_names.json`: رکوردهای پژوهش‌شده دستی؛ از 1.2.0 فیلدبه‌فیلد با base ادغام می‌شوند.
- `app/src/main/assets/data_sources.json`: رجیستری ماشین‌خوان منابع.
- `app/src/main/assets/data_build_info.json`: آمار واقعی پوشش پس از build/sync.

## امضای Release

کلید خصوصی واقعی در GitHub عمومی قرار نمی‌گیرد. برای جزئیات `signing/README.md` را بخوانید. برای آپدیت‌های آینده `applicationId` و کلید signing باید ثابت بمانند.

## مستندات

`PROJECT_INFO.md`، `DATA_CATALOG.md`، `DATA_MODEL.md`، `DATA_SOURCES.md`، `ROADMAP.md`، `RELEASE_CHECKLIST.md` و `THIRD_PARTY_NOTICES.md`.

## تاریخچه کوتاه

### 1.0.1
- فهرست کامل پایه در سورس persist شد.
- صفحه «همه نام‌ها» اضافه شد.

### 1.0.2
- placeholderهای «نیازمند بررسی» از معنی/ریشه حذف شدند.
- دسته خنثی «فهرست عمومی ایران» اضافه شد.
- نوشتار لاتین کمکی اضافه شد.

### 1.1.0
- هسته داده به aggregator چندمنبعی ارتقا یافت.
- provenance، چند transliteration، Wikidata، Wiktionary/Kaikki و گزارش پوشش اضافه شدند.

### 1.1.1
- دیتاست MIT `mohammadhejazirad/persian-names` با حدود ۱۰٬۰۸۸ نام یکتا به pipeline افزوده شد.
- منبع داده مبتنی بر اطلاعات لو‌رفته بانکی به‌صراحت کنار گذاشته شد.

### 1.2.0
- لایه واژگانی `Maani/Dehkhoda-Lexicon` با مجوز CC BY-SA 4.0 اضافه شد.
- هم‌معنی/متضاد واژه هم‌نوشت با نام در فیلد و UI مستقل نمایش داده می‌شود و با معنی شخص‌نام مخلوط نمی‌شود.
- curated/base به‌صورت فیلدبه‌فیلد merge می‌شوند تا enrichment چندمنبعی حفظ شود.
