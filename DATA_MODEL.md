# Data Model

## NameEntry

فیلدهای runtime:

- `name`
- `gender`
- `meaning`
- `origin`
- `usageCultureIds`
- `latin`
- `latinVariants`
- `pronunciation`
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

## Provenance

`sourceIds` مشخص می‌کند کدام منبع‌ها در ساخت یک رکورد نقش داشته‌اند. `classificationSourceIds` مشخص می‌کند انتساب زبانی/فرهنگی از چه منبع ساختاری آمده است.

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

`origin` با `usageCultureIds` یکی نیست.

برای مثال، P407 در Wikidata فقط «language of work or name» است و در App-NameDic به‌عنوان سیگنال طبقه‌بندی زبانی استفاده می‌شود؛ این مقدار به‌صورت خودکار به `origin` تبدیل نمی‌شود.

پادشاهان، وزیران، دانشمندان و شاعران مدل تاریخی مستقل خواهند داشت؛ «نام شخص» با «زندگینامه شخصیت» یک مفهوم نیست.
