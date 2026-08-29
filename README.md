# App-NameDic — نام‌نامه ایران

«نام‌نامه ایران» یک برنامه Android آفلاین برای جستجو، بررسی، پسندیدن و مقایسه نام‌های ایرانی است. تمرکز برنامه روی انتخاب عملی اسم است، نه فقط نمایش یک فهرست خام.

## نسخه فعلی: 1.4.0

- Kotlin + Jetpack Compose + Material 3
- minSdk 24 / targetSdk 36 / compileSdk 36
- AGP 9.3.0 / Gradle 9.5.0 / Java 17
- دیتابیس آفلاین چندمنبعی با بیش از ۲۶ هزار رکورد نام
- تفکیک مستقیم اسم‌های دخترانه و پسرانه
- جستجو در نام، معنی، ریشه، داده واژگانی و نوشتار لاتین
- صفحه «اسم پیدا کن» برای مرور انتخاب‌محور نام‌ها
- علاقه‌مندی‌های آفلاین با SharedPreferences
- لیست نهایی انتخاب اسم
- انتخاب ۲ تا ۴ اسم از پسندیده‌ها برای مقایسه
- مقایسه کنارهم جنسیت، معنی، ریشه، نوشتار لاتین و فرهنگ‌های ثبت‌شده
- نام‌های مشابه بر اساس جنسیت، حرف اول، فرهنگ مشترک و غنای داده
- فرهنگ‌ها و زبان‌ها فقط زمانی نمایش داده می‌شوند که واقعاً داده داشته باشند
- Drawer مشترک پروژه با پروفایل و انتخاب تصویر
- Back navigation بدون خروج ناخواسته از صفحات داخلی
- About بدون نمایش package name
- Core Library Desugaring برای سازگاری بهتر Android 7.x
- بررسی اختیاری نسخه جدید؛ استفاده روزمره برنامه offline-first است

## اصل حیاتی داده‌ها

سه مفهوم مستقل نگه داشته می‌شوند:

1. `meaning` = معنی یا توضیح مستقیم خود شخص‌نام با منبع مستقیم.
2. `origin` = ریشه زبانی یا تاریخی منبع‌دار.
3. `lexicalMeaningFa` / `lexicalAntonymsFa` = اطلاعات واژه فارسی هم‌نوشت با نام.

هم‌نوشت بودن یک واژه با نام، رایج بودن نام در یک فرهنگ، و ریشه زبانی آن سه ادعای متفاوت‌اند. برنامه هیچ‌کدام را از روی دیگری حدس نمی‌زند.

## منابع و pipeline داده

منابع خودکار فقط زمانی وارد corpus می‌شوند که مجوز و provenance روشن داشته باشند. منابع فعلی شامل چند دیتاست نام ایرانی/فارسی، Wikidata، Wiktionary/Kaikki و Maani/Dehkhoda-Lexicon هستند. سامانه تعاملی ثبت احوال به‌عنوان مرجع رسمی بررسی می‌شود، اما تا زمانی که API عمومی و شرایط برداشت ماشینی روشن نباشد bulk-scrape نمی‌شود.

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
- `app/src/main/assets/curated_names.json`: رکوردهای پژوهش‌شده دستی که فیلدبه‌فیلد با base ادغام می‌شوند.
- `app/src/main/assets/data_sources.json`: رجیستری ماشین‌خوان منابع.
- `app/src/main/assets/data_build_info.json`: آمار واقعی پوشش پس از build/sync.

## امضای Release

کلید خصوصی واقعی در GitHub عمومی قرار نمی‌گیرد. برای آپدیت‌های آینده `applicationId` و کلید signing باید ثابت بمانند. جزئیات در `signing/README.md` توضیح داده شده است.

## تاریخچه نسخه‌ها

### 1.4.0
- «لیست نهایی انتخاب اسم» به صفحه پسندیده‌ها اضافه شد.
- کاربر می‌تواند ۲ تا ۴ اسم را انتخاب و در کارت‌های کنارهم مقایسه کند.
- مقایسه شامل جنسیت، معنی، ریشه، نوشتار لاتین، فرهنگ و داده واژگانی موجود است.
- شناسه‌های داخلی فرهنگ در UI جزئیات نمایش داده نمی‌شوند و عنوان فارسی استفاده می‌شود.
- متن‌های مربوط به نبود معنی/ریشه طوری نوشته شده‌اند که نبود داده را با نامعتبر بودن اسم اشتباه نگیرند.

### 1.3.1
- دو رابط Compose قدیمی و بلااستفاده حذف شدند.
- جستجوی خالی دیگر کل corpus را بارگذاری نمی‌کند.
- Core Library Desugaring برای Android 7.x فعال شد.
- metadata نسخه و updater هماهنگ شد.

### 1.3.0
- رابط انتخاب‌محور جدید با Home، Search، Discover، Favorites و Drawer جدید فعال شد.
- ورود مستقیم به لیست دخترانه و پسرانه اضافه شد.
- صفحه‌های خالی راهنما‌دار، پیشنهاد روز، نام‌های مشابه و فرهنگ‌های دارای داده اضافه شدند.

### 1.2.0
- لایه واژگانی `Maani/Dehkhoda-Lexicon` با مجوز CC BY-SA 4.0 اضافه شد.
- curated/base به‌صورت فیلدبه‌فیلد merge شدند تا enrichment چندمنبعی حفظ شود.

### 1.1.x
- هسته داده به aggregator چندمنبعی ارتقا یافت.
- provenance، چند transliteration، Wikidata، Wiktionary/Kaikki و دیتاست MIT `mohammadhejazirad/persian-names` اضافه شدند.

### 1.0.x
- فهرست پایه نام‌ها، علاقه‌مندی، دسته‌بندی و ساختار اولیه برنامه ایجاد شد.
