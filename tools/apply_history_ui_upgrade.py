#!/usr/bin/env python3
"""Connect the standalone historical-figures module to the main Compose UI.

The migration is deliberately idempotent. CI may run it again on a source tree that
already contains the v1.5.0 changes; in that case each block is skipped instead of
being duplicated.
"""

from pathlib import Path

UI_PATH = Path("app/src/main/java/ir/asteam/namedic/ui/NameDicRedesign.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    """Replace one exact old block, or accept that the new block already exists."""
    if new in text:
        print(f"[skip] {label}: already applied")
        return text
    if old not in text:
        raise SystemExit(f"[history-ui] ERROR: {label}: expected block not found")
    print(f"[apply] {label}")
    return text.replace(old, new, 1)


def main() -> None:
    text = UI_PATH.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "private enum class NewScreen { HOME, GIRLS, BOYS, SEARCH, DISCOVER, FAVORITES, CULTURES, CULTURE_NAMES, DETAIL, ABOUT, CONTACT }",
        "private enum class NewScreen { HOME, GIRLS, BOYS, SEARCH, DISCOVER, FAVORITES, CULTURES, HISTORY, CULTURE_NAMES, DETAIL, ABOUT, CONTACT }",
        "add history destination",
    )

    text = replace_once(
        text,
        "if (screen !in setOf(NewScreen.DETAIL, NewScreen.ABOUT, NewScreen.CONTACT, NewScreen.CULTURE_NAMES)) {",
        "if (screen !in setOf(NewScreen.DETAIL, NewScreen.ABOUT, NewScreen.CONTACT, NewScreen.CULTURE_NAMES, NewScreen.HISTORY)) {",
        "hide bottom navigation in history module",
    )

    text = replace_once(
        text,
        "                    NewScreen.CULTURES -> NewCultureScreen(discovery) {\n                        selectedCulture = it\n                        go(NewScreen.CULTURE_NAMES)\n                    }\n                    NewScreen.CULTURE_NAMES -> CultureNamesScreen(selectedCulture, discovery, favorites, ::toggleFavorite, ::openName)",
        "                    NewScreen.CULTURES -> NewCultureScreen(discovery) {\n                        selectedCulture = it\n                        go(NewScreen.CULTURE_NAMES)\n                    }\n                    NewScreen.HISTORY -> HistoricalFiguresScreen()\n                    NewScreen.CULTURE_NAMES -> CultureNamesScreen(selectedCulture, discovery, favorites, ::toggleFavorite, ::openName)",
        "render history screen",
    )

    text = replace_once(
        text,
        "    NewScreen.CULTURES -> \"فرهنگ‌ها و زبان‌ها\"\n    NewScreen.CULTURE_NAMES -> culture?.titleFa ?: \"اسم‌ها\"",
        "    NewScreen.CULTURES -> \"فرهنگ‌ها و زبان‌ها\"\n    NewScreen.HISTORY -> \"بزرگان تاریخ ایران\"\n    NewScreen.CULTURE_NAMES -> culture?.titleFa ?: \"اسم‌ها\"",
        "add history title",
    )

    text = replace_once(
        text,
        "            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {\n                MiniTool(\"جستجوی دقیق\", Icons.Rounded.Search, Modifier.weight(1f)) { go(NewScreen.SEARCH) }\n                MiniTool(\"اسم پیدا کن\", Icons.Rounded.AutoAwesome, Modifier.weight(1f)) { go(NewScreen.DISCOVER) }\n                MiniTool(\"فرهنگ‌ها\", Icons.Rounded.Language, Modifier.weight(1f)) { go(NewScreen.CULTURES) }\n            }\n        }\n\n        if (cultureStats.isNotEmpty()) {",
        "            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {\n                MiniTool(\"جستجوی دقیق\", Icons.Rounded.Search, Modifier.weight(1f)) { go(NewScreen.SEARCH) }\n                MiniTool(\"اسم پیدا کن\", Icons.Rounded.AutoAwesome, Modifier.weight(1f)) { go(NewScreen.DISCOVER) }\n                MiniTool(\"فرهنگ‌ها\", Icons.Rounded.Language, Modifier.weight(1f)) { go(NewScreen.CULTURES) }\n            }\n        }\n\n        item { HistoricalFiguresHomeCard { go(NewScreen.HISTORY) } }\n\n        if (cultureStats.isNotEmpty()) {",
        "add history card to home",
    )

    text = replace_once(
        text,
        "        DrawerItem(\"فرهنگ‌ها و زبان‌ها\", Icons.Rounded.Language) { onNavigate(NewScreen.CULTURES) }\n        DrawerItem(\"اسم‌های پسندیده\", Icons.Rounded.Favorite) { onNavigate(NewScreen.FAVORITES) }",
        "        DrawerItem(\"فرهنگ‌ها و زبان‌ها\", Icons.Rounded.Language) { onNavigate(NewScreen.CULTURES) }\n        DrawerItem(\"بزرگان تاریخ ایران\", Icons.Rounded.AccountBalance) { onNavigate(NewScreen.HISTORY) }\n        DrawerItem(\"اسم‌های پسندیده\", Icons.Rounded.Favorite) { onNavigate(NewScreen.FAVORITES) }",
        "add history drawer item",
    )

    text = replace_once(
        text,
        "        item { Text(\"نام‌نامه ایران برای پیدا کردن و مقایسه اسم‌های دخترانه و پسرانه طراحی شده است. جستجو، فیلتر، پسندیده‌ها، پیشنهاد اسم، فرهنگ‌های دارای داده و اطلاعات معنی/ریشه در محیطی ساده و آفلاین در دسترس هستند.\") }",
        "        item { Text(\"نام‌نامه ایران برای پیدا کردن و مقایسه اسم‌های دخترانه و پسرانه طراحی شده است. جستجو، فیلتر، پسندیده‌ها، پیشنهاد اسم، فرهنگ‌های دارای داده، اطلاعات معنی/ریشه و بخش آفلاین «بزرگان تاریخ ایران» در یک محیط ساده در دسترس هستند.\") }",
        "refresh about description",
    )

    UI_PATH.write_text(text, encoding="utf-8")
    print("[history-ui] v1.5.0 history module connected to the main UI")


if __name__ == "__main__":
    main()
