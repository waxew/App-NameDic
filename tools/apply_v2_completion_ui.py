#!/usr/bin/env python3
"""Connect the independently compiled v2 feature modules to NameDicRedesign.kt.

The active Compose root is still a large legacy file. To keep GitHub connector edits
safe, this migration makes small exact replacements and is intentionally idempotent.
CI runs it before compilation and only persists the migrated UI after a successful
build on main.
"""

from __future__ import annotations

from pathlib import Path

UI_FILE = Path("app/src/main/java/ir/asteam/namedic/ui/NameDicRedesign.kt")


def patch_once(text: str, label: str, old: str, new: str, marker: str) -> str:
    """Replace one known block, or skip when the target marker is already present."""
    if marker in text:
        print(f"[v2-ui] skip {label}: already applied")
        return text
    if old not in text:
        raise SystemExit(f"[v2-ui] ERROR: expected block for {label!r} was not found")
    print(f"[v2-ui] apply {label}")
    return text.replace(old, new, 1)


def main() -> None:
    text = UI_FILE.read_text(encoding="utf-8")

    text = patch_once(
        text,
        "navigation destinations",
        "private enum class NewScreen { HOME, GIRLS, BOYS, SEARCH, DISCOVER, FAVORITES, CULTURES, HISTORY, CULTURE_NAMES, DETAIL, ABOUT, CONTACT }",
        "private enum class NewScreen { HOME, GIRLS, BOYS, SEARCH, DISCOVER, FAVORITES, CULTURES, TOOLS, HISTORY, CULTURE_NAMES, DETAIL, SETTINGS, ABOUT, CONTACT }",
        "NewScreen.TOOLS",
    )

    text = patch_once(
        text,
        "update preference state",
        '    var userName by remember { mutableStateOf(prefs.getString("user_name", "کاربر نام‌نامه").orEmpty()) }\n',
        '    var userName by remember { mutableStateOf(prefs.getString("user_name", "کاربر نام‌نامه").orEmpty()) }\n'
        '    var autoUpdateCheck by remember {\n'
        '        mutableStateOf(prefs.getBoolean("auto_update_check", true))\n'
        '    }\n',
        'prefs.getBoolean("auto_update_check", true)',
    )

    text = patch_once(
        text,
        "update notification host",
        "    ModalNavigationDrawer(\n",
        "    // بررسی نسخه اختیاری است و شکست شبکه هیچ مسیر آفلاینی را مسدود نمی‌کند.\n"
        "    UpdateNotificationHost(autoUpdateCheck)\n\n"
        "    ModalNavigationDrawer(\n",
        "UpdateNotificationHost(autoUpdateCheck)",
    )

    text = patch_once(
        text,
        "bottom navigation visibility",
        "                if (screen !in setOf(NewScreen.DETAIL, NewScreen.ABOUT, NewScreen.CONTACT, NewScreen.CULTURE_NAMES, NewScreen.HISTORY)) {",
        "                if (screen !in setOf(NewScreen.DETAIL, NewScreen.SETTINGS, NewScreen.ABOUT, NewScreen.CONTACT, NewScreen.CULTURE_NAMES, NewScreen.TOOLS, NewScreen.HISTORY)) {",
        "NewScreen.CULTURE_NAMES, NewScreen.TOOLS, NewScreen.HISTORY",
    )

    text = patch_once(
        text,
        "tools and history routes",
        "                    NewScreen.HISTORY -> HistoricalFiguresScreen()",
        "                    NewScreen.TOOLS -> AdvancedNameToolsScreen(repository, discovery, favorites, ::toggleFavorite, ::openName)\n"
        "                    NewScreen.HISTORY -> IranHistoryHubScreen()",
        "NewScreen.TOOLS -> AdvancedNameToolsScreen",
    )

    text = patch_once(
        text,
        "settings route",
        "                    NewScreen.CONTACT -> NewContact()\n",
        "                    NewScreen.CONTACT -> NewContact()\n"
        "                    NewScreen.SETTINGS -> SettingsScreen(autoUpdateCheck) { enabled ->\n"
        "                        autoUpdateCheck = enabled\n"
        "                        prefs.edit().putBoolean(\"auto_update_check\", enabled).apply()\n"
        "                    }\n",
        "NewScreen.SETTINGS -> SettingsScreen",
    )

    text = patch_once(
        text,
        "tools title",
        '    NewScreen.CULTURES -> "فرهنگ‌ها و زبان‌ها"\n    NewScreen.HISTORY -> "بزرگان تاریخ ایران"',
        '    NewScreen.CULTURES -> "فرهنگ‌ها و زبان‌ها"\n    NewScreen.TOOLS -> "ابزار انتخاب اسم"\n    NewScreen.HISTORY -> "تاریخ ایران"',
        'NewScreen.TOOLS -> "ابزار انتخاب اسم"',
    )

    text = patch_once(
        text,
        "settings title",
        '    NewScreen.DETAIL -> name?.name ?: "جزئیات اسم"\n    NewScreen.ABOUT -> "درباره نام‌نامه"',
        '    NewScreen.DETAIL -> name?.name ?: "جزئیات اسم"\n    NewScreen.SETTINGS -> "تنظیمات"\n    NewScreen.ABOUT -> "درباره نام‌نامه"',
        'NewScreen.SETTINGS -> "تنظیمات"',
    )

    text = patch_once(
        text,
        "advanced tools home card",
        "        item { HistoricalFiguresHomeCard { go(NewScreen.HISTORY) } }",
        "        item { AdvancedToolsHomeCard { go(NewScreen.TOOLS) } }\n\n"
        "        item { HistoricalFiguresHomeCard { go(NewScreen.HISTORY) } }",
        "AdvancedToolsHomeCard { go(NewScreen.TOOLS) }",
    )

    text = patch_once(
        text,
        "name share and report actions",
        "        if (related.isNotEmpty()) {",
        "        item { NameShareReportActions(entry, cultureTitles) }\n\n"
        "        if (related.isNotEmpty()) {",
        "NameShareReportActions(entry, cultureTitles)",
    )

    text = patch_once(
        text,
        "drawer settings and share",
        '        HorizontalDivider()\n        DrawerItem("خانه", Icons.Rounded.Home) { onNavigate(NewScreen.HOME) }',
        '        HorizontalDivider()\n'
        '        DrawerItem("تنظیمات", Icons.Rounded.Settings) { onNavigate(NewScreen.SETTINGS) }\n'
        '        ShareAppDrawerItem()\n'
        '        HorizontalDivider()\n'
        '        DrawerItem("خانه", Icons.Rounded.Home) { onNavigate(NewScreen.HOME) }',
        'DrawerItem("تنظیمات", Icons.Rounded.Settings)',
    )

    text = patch_once(
        text,
        "drawer advanced tools",
        '        DrawerItem("اسم پیدا کن", Icons.Rounded.AutoAwesome) { onNavigate(NewScreen.DISCOVER) }\n        DrawerItem("فرهنگ‌ها و زبان‌ها", Icons.Rounded.Language) { onNavigate(NewScreen.CULTURES) }',
        '        DrawerItem("اسم پیدا کن", Icons.Rounded.AutoAwesome) { onNavigate(NewScreen.DISCOVER) }\n'
        '        DrawerItem("پیشنهادگر پیشرفته", Icons.Rounded.Tune) { onNavigate(NewScreen.TOOLS) }\n'
        '        DrawerItem("فرهنگ‌ها و زبان‌ها", Icons.Rounded.Language) { onNavigate(NewScreen.CULTURES) }',
        'DrawerItem("پیشنهادگر پیشرفته", Icons.Rounded.Tune)',
    )

    text = patch_once(
        text,
        "about v2 description",
        '        item { Text("نام‌نامه ایران برای پیدا کردن و مقایسه اسم‌های دخترانه و پسرانه طراحی شده است. جستجو، فیلتر، پسندیده‌ها، پیشنهاد اسم، فرهنگ‌های دارای داده، اطلاعات معنی/ریشه و بخش آفلاین «بزرگان تاریخ ایران» در یک محیط ساده در دسترس هستند.") }',
        '        item { Text("نام‌نامه ایران برای پیدا کردن، مقایسه و شناخت اسم‌های ایرانی طراحی شده است. جستجو، پیشنهادگر پیشرفته، پسندیده‌ها و مقایسه، فرهنگ‌های دارای داده، معنی/ریشه، مرکز تاریخ ایران با شخصیت‌ها و خط زمانی، آزمون آفلاین و ابزار گزارش داده در یک محیط یکپارچه در دسترس هستند.") }',
        "مرکز تاریخ ایران با شخصیت‌ها و خط زمانی",
    )

    text = patch_once(
        text,
        "about data transparency",
        '        item { Text("نسخه ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold) }',
        '        item { DataTransparencySection() }\n'
        '        item { Text("نسخه ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold) }',
        "item { DataTransparencySection() }",
    )

    UI_FILE.write_text(text, encoding="utf-8")
    print("[v2-ui] completed successfully")


if __name__ == "__main__":
    main()
