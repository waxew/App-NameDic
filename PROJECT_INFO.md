# Project Info

- Repository: `App-NameDic`
- User-facing name: `نام‌نامه ایران`
- Application ID / namespace: `ir.asteam.namedic`
- Version: `1.2.0` (`versionCode=6`)
- Kotlin + Jetpack Compose + Material 3
- AGP 9.3.0 / Gradle 9.5.0 / JDK 17
- minSdk: 24
- targetSdk: 36
- compileSdk: 36

معماری برنامه offline-first است: `MainActivity`، رابط Compose در `ui/App.kt`، مخزن داده در `data/NameRepository.kt`، بررسی نسخه در `data/UpdateChecker.kt` و مدل‌ها در `model/Models.kt`.

از نسخه 1.1.0 لایه داده از چند منبع دارای مجوز روشن ساخته می‌شود. `tools/sync_names.py` فهرست‌های باز را ادغام می‌کند، `tools/sync_wikidata_cultures.py` طبقه‌بندی ساختاری را اضافه می‌کند و `tools/sync_wiktionary_names.py` داده‌های ساختاری Wiktionary/Kaikki مثل تلفظ و romanization را enrich می‌کند.

در نسخه 1.1.1 منبع MIT `mohammadhejazirad/persian-names` با parser متنی مستقل به رأی‌گیری جنسیت و پوشش نام‌ها اضافه شد.

در نسخه 1.2.0 لایه واژگانی جداگانه اضافه شده است. `tools/sync_dehkhoda_lexical.py` از دیتاست CC BY-SA 4.0 `Maani/Dehkhoda-Lexicon` فقط هم‌معنی/متضاد واژه فارسی هم‌نوشت با نام را استخراج می‌کند. این داده در `lexicalMeaningFa` و `lexicalAntonymsFa` ذخیره می‌شود و هرگز به‌صورت خودکار جای `meaning` یا `origin` شخص‌نام قرار نمی‌گیرد.

از 1.2.0 رکورد curated نیز فیلدبه‌فیلد با base ادغام می‌شود تا داده‌های چندمنبعی مثل تلفظ، provenance و اطلاعات واژگانی هنگام تکمیل دستی یک نام از بین نرود.

علاقه‌مندی و URI عکس پروفایل در SharedPreferences نگهداری می‌شوند. applicationId و signing ثابت می‌مانند تا آپدیت روی نسخه قبلی نصب شود و داده کاربر حفظ شود.
