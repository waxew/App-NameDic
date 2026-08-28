# Data Model

## NameEntry

فیلدها: `name`, `gender`, `meaning`, `origin`, `usageCultureIds`, `latin`, `pronunciation`, `tags`, `sourceTitle`, `sourceUrl`, `verificationStatus`, `notes`.

## VerificationStatus

- `VERIFIED`: منبع مشخص و بررسی کامل
- `CURATED`: گردآوری و بازبینی اولیه
- `BASE_ONLY`: فقط نام/جنسیت از فهرست پایه
- `NEEDS_REVIEW`: نیازمند منبع یا بررسی بیشتر

## قاعده حیاتی

`origin` با `usageCultureIds` یکی نیست. محل رواج نباید به ریشه جعلی تبدیل شود.

پادشاهان، وزیران، دانشمندان و شاعران در نسخه‌های بعدی مدل تاریخی مستقل خواهند داشت؛ «نام شخص» با «زندگینامه شخصیت» یک مفهوم نیست.
