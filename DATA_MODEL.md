# Data Model

## NameEntry

فیلدهای runtime:

- `name`
- `gender`
- `meaning` — معنی/توضیح مستقیم خود شخص‌نام؛ فقط با منبع مستقیم
- `origin` — ریشه زبانی/تاریخی منبع‌دار
- `usageCultureIds` — زبان‌ها/فرهنگ‌ها یا فهرست‌هایی که نام در آن‌ها مستند شده
- `latin`
- `latinVariants`
- `pronunciation`
- `lexicalMeaningFa` — هم‌معنی‌های واژه فارسی هم‌نوشت با نام، نه معنی قطعی شخص‌نام
- `lexicalAntonymsFa` — متضادهای همان واژه فارسی
- `tags`
- `sourceIds`
- `sourceTitle`
- `sourceUrl`
- `verificationStatus`
- `notes`

فایل `names_base.json` علاوه بر این‌ها می‌تواند metadata ساخت را نگه دارد:

- `sourceCount`
- `classificationSourceIds`
- `wikidataIds`
- `wiktionaryPage`
- `genderConflict`

## سه مفهوم مستقل

### Name meaning / معنی شخص‌نام
`meaning` فقط زمانی پر می‌شود که منبع مشخصی مستقیماً برای خود نام معنی ارائه کند. معنی واژه مشابه یا هم‌نوشت به این فیلد منتقل نمی‌شود.

### Origin / ریشه
`origin` منشأ زبانی/تاریخی نام یا واژه است و با محل رواج یکی نیست.

### Lexical homonym / واژه هم‌نوشت
`lexicalMeaningFa` و `lexicalAntonymsFa` اطلاعات واژگانی یک واژه فارسی هستند که املای آن با نام یکسان است. این لایه برای کمک پژوهشی و جستجو مفید است اما به‌تنهایی معنی یا ریشه شخص‌نام را اثبات نمی‌کند.

## Provenance

`sourceIds` مشخص می‌کند کدام منبع‌ها در ساخت یک رکورد نقش داشته‌اند. `classificationSourceIds` مشخص می‌کند انتساب زبانی/فرهنگی از چه منبع ساختاری آمده است.

از نسخه 1.2.0 داده curated به‌صورت فیلدبه‌فیلد با base ادغام می‌شود؛ بنابراین تکمیل دستی معنی یک نام، تلفظ یا provenance چندمنبعی همان نام را حذف نمی‌کند.

## Gender

- `MALE`
- `FEMALE`
- `UNISEX`
- `UNKNOWN`

اگر منابع معتبر باز هم‌زمان برای یک نام زن/مرد گزارش متعارض بدهند، aggregator آن را بدون حدس `UNISEX` می‌کند و `genderConflict=true` می‌گذارد تا بعداً بازبینی شود.

## VerificationStatus

- `VERIFIED`: منبع مشخص و بررسی کامل
- `CURATED`: گردآوری/غنی‌سازی ساختاری
- `BASE_ONLY`: فقط داده پایه
- `NEEDS_REVIEW`: نیازمند منبع یا بررسی بیشتر

## قاعده حیاتی

`origin` با `usageCultureIds` یکی نیست و هر دو نیز از `lexicalMeaningFa` مستقل هستند.

برای مثال، P407 در Wikidata فقط «language of work or name» است و در App-NameDic به‌عنوان سیگنال طبقه‌بندی زبانی استفاده می‌شود؛ این مقدار به‌صورت خودکار به `origin` تبدیل نمی‌شود.

پادشاهان، وزیران، دانشمندان و شاعران مدل تاریخی مستقل خواهند داشت؛ «نام شخص» با «زندگینامه شخصیت» یک مفهوم نیست.
