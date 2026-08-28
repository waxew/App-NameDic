#!/usr/bin/env python3
"""اصلاح کیفیت داده/نمایش برای v1.0.2.

- همه رکوردهای پایه در دسته خنثی «فهرست عمومی ایران» دیده می‌شوند.
- معنی/ریشه ناشناخته با متن ساختگی پر نمی‌شود.
- UI فقط فیلدهای واقعاً موجود را نمایش می‌دهد و وضعیت خام پژوهش را روی کارت‌ها تحمیل نمی‌کند.
- نوشتار لاتینِ همگام‌شده از names_base.json خوانده می‌شود.
"""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT / "app/src/main/java/ir/asteam/namedic/data/NameRepository.kt"
APP = ROOT / "app/src/main/java/ir/asteam/namedic/ui/App.kt"
GRADLE = ROOT / "app/build.gradle.kts"
VERSION = ROOT / "VERSION.txt"
PROJECT = ROOT / "PROJECT_INFO.md"
README = ROOT / "README.md"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"marker not found for {label}")
    return text.replace(old, new, 1)


def patch_repository() -> None:
    text = REPO.read_text(encoding="utf-8")

    text = replace_once(
        text,
        '    val cultures: List<CultureCategory> = listOf(\n        CultureCategory("persian",',
        '    val cultures: List<CultureCategory> = listOf(\n        CultureCategory("iran_general", "فهرست عمومی ایران", "نام‌های ثبت‌شده در دیتاست عمومی ایران؛ این دسته ادعای ریشه زبانی ندارد"),\n        CultureCategory("persian",',
        "general culture",
    )

    old = '                    val name = item.optString("name", key).ifBlank { key }\n                    add(NameEntry(name, parseGender(item.optString("gender")), "معنی و ریشه این نام هنوز در بخش پژوهش در حال تکمیل است.", "نیازمند بررسی", emptyList(), sourceTitle = "Persian Names dataset (MIT)", verificationStatus = VerificationStatus.BASE_ONLY))'
    new = '                    val name = item.optString("name", key).ifBlank { key }\n                    add(\n                        NameEntry(\n                            name = name,\n                            gender = parseGender(item.optString("gender")),\n                            meaning = "",\n                            origin = "",\n                            usageCultureIds = listOf("iran_general"),\n                            latin = item.optString("latin"),\n                            sourceTitle = "Persian Names dataset (MIT)",\n                            verificationStatus = VerificationStatus.BASE_ONLY,\n                        )\n                    )'
    text = replace_once(text, old, new, "base record")
    REPO.write_text(text, encoding="utf-8")


def patch_app() -> None:
    text = APP.read_text(encoding="utf-8")

    old_row = '''                Text(\n                    "${entry.gender.titleFa} • ${entry.meaning}",\n                    maxLines = 2,\n                    style = MaterialTheme.typography.bodySmall,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant,\n                )'''
    new_row = '''                val subtitle = buildList {\n                    add(entry.gender.titleFa)\n                    if (entry.meaning.isNotBlank()) add(entry.meaning)\n                    else if (entry.latin.isNotBlank()) add(entry.latin)\n                }.joinToString(" • ")\n                Text(\n                    subtitle,\n                    maxLines = 2,\n                    style = MaterialTheme.typography.bodySmall,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant,\n                )'''
    text = replace_once(text, old_row, new_row, "name row")

    old_culture = '''        .map { it.titleFa }\n        .ifEmpty { listOf("هنوز دسته‌بندی نشده") }'''
    new_culture = '''        .map { it.titleFa }'''
    text = replace_once(text, old_culture, new_culture, "culture labels")

    old_info = '''                    InfoRow("جنسیت", entry.gender.titleFa)\n                    InfoRow("معنی / توضیح", entry.meaning)\n                    InfoRow("ریشه", entry.origin)\n                    InfoRow("فرهنگ استفاده", cultureNames.joinToString("، "))\n                    InfoRow("وضعیت داده", entry.verificationStatus.titleFa)\n                    if (entry.pronunciation.isNotBlank()) InfoRow("تلفظ", entry.pronunciation)\n                    if (entry.tags.isNotEmpty()) InfoRow("برچسب‌ها", entry.tags.joinToString("، "))\n                    if (entry.sourceTitle.isNotBlank()) InfoRow("منبع", entry.sourceTitle)\n                    if (entry.notes.isNotBlank()) InfoRow("یادداشت پژوهش", entry.notes)'''
    new_info = '''                    InfoRow("جنسیت", entry.gender.titleFa)\n                    if (entry.meaning.isNotBlank()) InfoRow("معنی / توضیح", entry.meaning)\n                    if (entry.origin.isNotBlank()) InfoRow("ریشه", entry.origin)\n                    if (cultureNames.isNotEmpty()) InfoRow("فرهنگ / فهرست", cultureNames.joinToString("، "))\n                    if (entry.latin.isNotBlank()) InfoRow("نوشتار لاتین", entry.latin)\n                    if (entry.pronunciation.isNotBlank()) InfoRow("تلفظ", entry.pronunciation)\n                    if (entry.tags.isNotEmpty()) InfoRow("برچسب‌ها", entry.tags.joinToString("، "))\n                    if (entry.sourceTitle.isNotBlank()) InfoRow("منبع فهرست", entry.sourceTitle)\n                    if (entry.notes.isNotBlank()) InfoRow("یادداشت پژوهش", entry.notes)\n                    if (entry.meaning.isBlank() && entry.origin.isBlank()) {\n                        Text(\n                            "برای معنی و ریشه این نام هنوز منبع معتبر به رکورد متصل نشده است.",\n                            style = MaterialTheme.typography.bodySmall,\n                            color = MaterialTheme.colorScheme.onSurfaceVariant,\n                        )\n                    }'''
    text = replace_once(text, old_info, new_info, "detail info")

    old_home = '                        "${repository.names.size} نام • ${repository.cultures.size} دسته فرهنگی/زبانی",'
    new_home = '                        "${repository.names.size} نام • ${repository.names.count { it.meaning.isNotBlank() }} رکورد دارای معنی • ${repository.cultures.size} دسته",'
    text = replace_once(text, old_home, new_home, "home stats")

    APP.write_text(text, encoding="utf-8")


def patch_versions() -> None:
    text = GRADLE.read_text(encoding="utf-8")
    text = text.replace('versionCode = 2', 'versionCode = 3').replace('versionName = "1.0.1"', 'versionName = "1.0.2"')
    GRADLE.write_text(text, encoding="utf-8")

    text = VERSION.read_text(encoding="utf-8")
    text = text.replace('versionCode=2', 'versionCode=3').replace('versionName=1.0.1', 'versionName=1.0.2')
    VERSION.write_text(text, encoding="utf-8")

    text = PROJECT.read_text(encoding="utf-8")
    text = text.replace('Version: `1.0.1` (`versionCode=2`)', 'Version: `1.0.2` (`versionCode=3`)')
    PROJECT.write_text(text, encoding="utf-8")

    text = README.read_text(encoding="utf-8")
    text = text.replace('## نسخه 1.0.1', '## نسخه 1.0.2', 1)
    note = '''\n\n## اصلاح 1.0.2\n\n- همه نام‌های پایه در دسته خنثی «فهرست عمومی ایران» قابل مرورند.\n- عبارت‌های placeholder مثل «نیازمند بررسی» از جزئیات عمومی حذف شده‌اند.\n- فیلدهای معنی/ریشه فقط وقتی نمایش داده می‌شوند که داده واقعی داشته باشند.\n- نوشتار لاتین برای رکوردهای قابل تطبیق از منبع آزاد جداگانه اضافه می‌شود.\n- دسته‌های قومی/زبانی همچنان فقط بر اساس داده گردآوری‌شده نمایش داده می‌شوند و به‌صورت حدسی پر نمی‌شوند.\n'''
    if '## اصلاح 1.0.2' not in text:
        text += note
    README.write_text(text, encoding="utf-8")


def main() -> None:
    patch_repository()
    patch_app()
    patch_versions()
    print("Applied App-NameDic v1.0.2 data-quality fix")


if __name__ == "__main__":
    main()
