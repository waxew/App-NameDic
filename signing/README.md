# Release signing

فایل واقعی keystore و رمزهای آن نباید وارد GitHub عمومی شوند.

در ZIP خصوصی تحویلی فایل‌های `App-NameDic-release.jks`، `signing.properties` و `PRIVATE_SIGNING_INFO.txt` قرار دارند. `app/build.gradle.kts` در صورت وجود فایل خصوصی signing، Release را با همان کلید امضا می‌کند.

برای نسخه‌های بعدی همین keystore را نگه دارید؛ گم‌شدن آن می‌تواند انتشار آپدیت سازگار با همان applicationId را غیرممکن کند.
