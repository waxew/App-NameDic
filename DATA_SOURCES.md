# منابع داده App-NameDic

هدف نسخه 2.0.0 ساخت یک corpus چندمنبعی و قابل‌ردیابی است، نه تکیه بر یک فایل نام یا تبدیل داده‌های تاریخی به دادهٔ نام. هر فیلد فقط از منبعی وارد می‌شود که واقعاً آن فیلد را ارائه می‌کند و شرایط استفادهٔ آن برای همان نوع مصرف روشن باشد.

## قواعد ادغام نام‌ها

- نام‌ها با نرمال‌سازی «ی/ي»، «ک/ك»، فاصله و نیم‌فاصله deduplicate می‌شوند.
- جنسیت از چند منبع رأی‌گیری می‌شود. تعارض زن/مرد به‌جای انتخاب حدسی، `UNISEX` و `genderConflict=true` ثبت می‌شود.
- `Origin / ریشه` با `Usage Culture / فرهنگ یا زبان استفاده` یکی نیست.
- `meaning` فقط برای معنی مستقیمِ شخص‌نام است و باید منبع مستقیم داشته باشد.
- `lexicalMeaningFa` و `lexicalAntonymsFa` فقط اطلاعات واژه فارسی هم‌نوشت با نام هستند و نباید به‌عنوان معنی یا ریشه قطعی شخص‌نام نمایش داده شوند.
- شناسه منبع هر رکورد در `sourceIds` نگهداری می‌شود.
- انتساب فرهنگی/زبانی مستند در `classificationSourceIds` ثبت می‌شود.
- معنی، ریشه یا قومیت از روی ظاهر یک نام حدس زده نمی‌شود.
- منبعی که مجوز روشن ندارد، حتی اگر دیتاست بزرگی داشته باشد، خودکار وارد corpus نام‌ها نمی‌شود.

## منابع خودکار نام‌ها

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

متن بلند مدخل‌ها و glossهای طولانی کپی نمی‌شوند. بخش داده مشتق‌شده از Wiktionary همراه attribution و شرایط ShareAlike/GFDL نگهداری می‌شود.

### Maani/Dehkhoda-Lexicon — CC BY-SA 4.0
این دیتاست روابط واژگانی فارسی دارد. importer فقط وقتی headword با یک نام موجود تطابق قطعی داشته باشد، داده را در فیلدهای مستقل `lexicalMeaningFa` و `lexicalAntonymsFa` ذخیره می‌کند. این اطلاعات «معنی واژه فارسی هم‌نوشت با نام» است و به‌تنهایی معنی، ریشه یا منشأ شخص‌نام را ثابت نمی‌کند.

## منابع مرجع، نه bulk import نام

### سامانه تعاملی نام سازمان ثبت احوال (سهیم)
مرجع اولویت‌دار برای نام‌های مجاز/ثبت‌شده در ایران، جنسیت، معنی، تلفظ، فراوانی و جزئیات نام است. تا زمانی که endpoint عمومی و مجوز برداشت ماشینی روشن نباشد، استفاده آن برای بررسی/تصحیح دستی است و هیچ محدودیت دسترسی دور زده نمی‌شود.

### Encyclopaedia Iranica
برای ریشه‌شناسی تاریخی، تاریخ ایران و زمینه علمی مرجع مهمی است. متن مقاله‌ها bulk-copy نمی‌شود.

## دادهٔ تاریخ ایران در 2.0.0

دادهٔ تاریخی از corpus نام‌ها کاملاً جداست. برنامه از اطلاعات یک شخصیت مشهور برای تعیین معنی، ریشه، جنسیت یا فرهنگ نام او استفاده نمی‌کند.

### `historical_figures.json`
بستهٔ پایهٔ ۲۳ شخصیت تاریخی. رکوردها شامل نام، دوره، دسته، نقش، سال‌ها، خلاصهٔ فارسی بازنویسی‌شده، نکات مهم و یک لینک مرجع هستند.

### `historical_figures_extra.json`
بستهٔ تکمیلی ۱۲ شخصیت که پوشش زنان اثرگذار، فرماندهان، دانشمندان، ادبیات و اصلاح‌گران را افزایش می‌دهد. مجموع نسخه 2.0.0 برابر ۳۵ شخصیت است.

### `history_timeline.json`
۱۲ دوره/سلسلهٔ اثرگذار از مادها تا پهلوی. `startSort` و `endSort` فقط برای مرتب‌سازی داخلی‌اند و متن قابل نمایش از `yearsFa` می‌آید تا درباره تاریخ‌های تقریبی دقت کاذب ایجاد نشود. UI نیز صریحاً توضیح می‌دهد که برخی حکومت‌ها هم‌زمان یا منطقه‌ای بوده‌اند.

### منابع مرجع تاریخ
- Encyclopaedia Iranica برای چند مدخل و دورهٔ تاریخی، از جمله منابع پایه درباره هخامنشیان، ساسانیان و برخی شخصیت‌ها.
- Wikipedia/Wikidata برای دادهٔ مرجع عمومی و لینک مطالعهٔ بیشتر در تعدادی از رکوردها.

متن مقاله‌های این منابع داخل APK کپی نمی‌شود. متن فارسی برنامه خلاصه و بازنویسی می‌شود و `sourceLabel/sourceUrl` برای traceability و مطالعهٔ بیشتر نگهداری می‌شود.

## منابع بررسی‌شده ولی bundle نشده برای نام‌ها

- `nikahd99/iranian-Names-Database-By-Gender`: دیتاست بزرگ، اما مجوز باز صریح پیدا نشد.
- `MansourM/persian-names-database`: مجوز باز صریح پیدا نشد؛ README آن منبع داده را یک فایل اکسل بیرونی معرفی می‌کند.
- `ehsan-shahbakhsh/api-iranian-names`: wrapper دارای MIT است، اما README اعلام می‌کند دیتاست از `MansourM/persian-names-database` اقتباس شده؛ تا روشن شدن حق بازنشر دیتاست اصلی وارد corpus نمی‌شود.
- `PJSoftCo/BabyNames`: معنی و زبان دارد، اما مجوز دیتاست روشن نیست.
- `ghaninia/databases`: داده scrape شده و مجوز باز صریح مشاهده نشد.
- `alisadeghiaghili/farsi-faker`: MIT است، ولی داده اصلی pickle است؛ فایل pickle شبکه در CI به‌دلیل ریسک اجرای کد unpickle نمی‌شود.
- `asghariali1/Iranian_names`: خود README اعلام می‌کند منبع از داده لو‌رفته بانکی ساخته شده؛ به دلایل حریم خصوصی و منشأ نامناسب استفاده نمی‌شود.
- CJKI Database of Persian Names: تجاری؛ فقط در صورت خرید مجوز می‌تواند اضافه شود.

## فایل‌های تولیدی و curated

- `app/src/main/assets/names_base.json`: corpus ادغام‌شده.
- `app/src/main/assets/data_build_info.json`: آمار پوشش واقعی.
- `app/src/main/assets/data_sources.json`: رجیستری ماشین‌خوان منابع نام.
- `app/src/main/assets/curated_names.json`: داده‌های پژوهش‌شده/دستی نام که به‌صورت فیلدبه‌فیلد روی base اولویت دارند.
- `app/src/main/assets/historical_figures.json`: بستهٔ پایه تاریخ.
- `app/src/main/assets/historical_figures_extra.json`: بستهٔ تکمیلی تاریخ.
- `app/src/main/assets/history_timeline.json`: Timeline آفلاین و منبع‌دار.

## اجرای pipeline و validatorها

```bash
python3 tools/sync_names.py
python3 tools/sync_mohammadhejazirad_names.py
python3 tools/restore_enriched_orphans.py /path/to/previous.json
python3 tools/sync_dehkhoda_lexical.py
python3 tools/sync_wikidata_cultures.py
python3 tools/sync_wiktionary_names.py
python3 tools/build_data_report.py
python3 tools/validate_data.py
python3 tools/validate_history.py
python3 tools/validate_history_timeline.py
```

همهٔ این کنترل‌های اصلی در GitHub Actions اجرا می‌شوند؛ خود اپ در استفاده روزمره همچنان offline-first است.
