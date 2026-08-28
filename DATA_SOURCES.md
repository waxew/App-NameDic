# منابع داده App-NameDic

هدف نسخه 1.1.x ساخت یک corpus چندمنبعی است، نه تکیه بر یک فایل نام. هر فیلد فقط از منبعی وارد می‌شود که واقعاً آن فیلد را ارائه می‌کند و مجوز استفاده آن روشن باشد.

## قواعد ادغام

- نام‌ها با نرمال‌سازی «ی/ي»، «ک/ك»، فاصله و نیم‌فاصله deduplicate می‌شوند.
- جنسیت از چند منبع رأی‌گیری می‌شود. تعارض زن/مرد به‌جای انتخاب حدسی، `UNISEX` و `genderConflict=true` ثبت می‌شود.
- `Origin / ریشه` با `Usage Culture / فرهنگ یا زبان استفاده` یکی نیست.
- شناسه منبع هر رکورد در `sourceIds` نگهداری می‌شود.
- انتساب فرهنگی/زبانی مستند در `classificationSourceIds` ثبت می‌شود.
- معنی، ریشه یا قومیت از روی ظاهر یک نام حدس زده نمی‌شود.
- منبعی که مجوز روشن ندارد، حتی اگر دیتاست بزرگی داشته باشد، خودکار وارد برنامه نمی‌شود.

## منابع خودکار

### nabidam/persian-names — MIT
پایه عمومی نام و جنسیت. این منبع 8,816 نام دارد و خودش معنی/ریشه ارائه نمی‌کند.

### farbodbj/persian-gender-by-name — Apache-2.0
فهرست بزرگ‌تر نام، جنسیت و نوشتار انگلیسی. برای افزایش پوشش نام و transliteration استفاده می‌شود.

### mehdi-haydari/iranianNames — MIT
نام فارسی، جنسیت و شکل انگلیسی. شکل انگلیسی فقط کمک جستجو/نمایش است و سند ریشه‌شناسی محسوب نمی‌شود.

### jadijadi/persianwords — CC0-1.0
فهرست نام دختر/پسر. فایل پسرانه در upstream چند merge marker دارد؛ importer فقط خط‌های معتبر نام را می‌پذیرد.

### armanyazdi/persian-names — MIT
فهرست‌های جداگانه نام فارسی دختر/پسر. فایل‌های انگلیسی upstream به‌صورت ردیفی با فایل فارسی هم‌تراز فرض نمی‌شوند.

### mohammadhejazirad/persian-names — MIT
حدود 10,088 نام یکتا در سه گروه مردانه، زنانه و مشترک. App-NameDic فقط ثابت‌های متنی تولیدشده در `src/generated-data.ts` را parse می‌کند؛ هیچ کد upstream اجرا نمی‌شود. این منبع فقط برای نام/جنسیت استفاده می‌شود و معنی یا ریشه از آن استنباط نمی‌شود.

### Wikidata — CC0
برای داده ساختاری استفاده می‌شود:
- ارتباط زبان نام با P407؛
- کلاس نام مردانه/زنانه/مشترک؛
- شناسه Q؛
- label لاتین در صورت تطابق.

P407 به‌عنوان «ریشه واژه» ذخیره نمی‌شود؛ فقط یک سیگنال طبقه‌بندی زبانی است.

### Wiktionary via Kaikki/Wiktextract — CC BY-SA + GFDL
فقط از مدخل‌های فارسی با `pos=name` و تطابق قطعی با نام محلی استفاده می‌شود. فیلدهای قابل استفاده:
- romanization؛
- IPA، با اولویت تلفظ برچسب‌خورده Iran؛
- جنسیت از categoryهای given name؛
- ثبت در دسته Persian given names؛
- ریشه کوتاه فقط وقتی category صریحی مثل `given names from Middle Persian` وجود دارد.

متن بلند مدخل‌ها و glossهای طولانی کپی نمی‌شوند. بخش داده مشتق‌شده از Wiktionary باید همراه attribution و شرایط ShareAlike مربوط نگهداری شود.

## منابع مرجع، نه bulk import

### سامانه تعاملی نام سازمان ثبت احوال (سهیم)
مرجع اولویت‌دار برای نام‌های مجاز/ثبت‌شده در ایران، جنسیت، معنی، تلفظ، فراوانی و جزئیات نام است. تا زمانی که endpoint عمومی و مجوز برداشت ماشینی روشن نباشد، استفاده آن برای بررسی/تصحیح دستی است و هیچ محدودیت دسترسی دور زده نمی‌شود.

### Encyclopaedia Iranica
برای ریشه‌شناسی تاریخی و زمینه علمی نام‌های ایرانی استفاده مرجع دارد. متن مقاله‌ها bulk-copy نمی‌شود.

## منابع بررسی‌شده ولی bundle نشده

- `nikahd99/iranian-Names-Database-By-Gender`: دیتاست بزرگ، اما مجوز باز صریح پیدا نشد.
- `MansourM/persian-names-database`: مجوز باز صریح پیدا نشد.
- `PJSoftCo/BabyNames`: معنی و زبان دارد، اما مجوز دیتاست روشن نیست.
- `ghaninia/databases`: داده scrape شده و مجوز باز صریح مشاهده نشد.
- `alisadeghiaghili/farsi-faker`: MIT است، ولی داده اصلی pickle است؛ فایل pickle شبکه در CI به‌دلیل ریسک اجرای کد unpickle نمی‌شود.
- `asghariali1/Iranian_names`: خود README اعلام می‌کند منبع از داده لو‌رفته بانکی ساخته شده؛ به دلایل حریم خصوصی و منشأ نامناسب استفاده نمی‌شود.
- CJKI Database of Persian Names: تجاری؛ فقط در صورت خرید مجوز می‌تواند اضافه شود.

## فایل‌های تولیدی

- `app/src/main/assets/names_base.json`: corpus ادغام‌شده.
- `app/src/main/assets/data_build_info.json`: آمار پوشش واقعی.
- `app/src/main/assets/data_sources.json`: رجیستری ماشین‌خوان منابع.
- `app/src/main/assets/curated_names.json`: داده‌های پژوهش‌شده/دستی که روی رکورد پایه اولویت دارند.

## اجرای pipeline

```bash
python3 tools/sync_names.py
python3 tools/sync_mohammadhejazirad_names.py
python3 tools/sync_wikidata_cultures.py
python3 tools/sync_wiktionary_names.py
python3 tools/build_data_report.py
python3 tools/validate_data.py
```

همه این مراحل در GitHub Actions اجرا می‌شوند؛ خود اپ در استفاده روزمره همچنان offline-first است.
