#!/usr/bin/env python3
"""Persist the full names dataset in source and expose it through an All Names screen.

This script is intentionally idempotent so CI can run it on every build.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/ir/asteam/namedic/ui/App.kt"
BUILD = ROOT / "app/build.gradle.kts"
VERSION = ROOT / "VERSION.txt"
README = ROOT / "README.md"
PROJECT_INFO = ROOT / "PROJECT_INFO.md"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"Could not patch {label}: expected marker not found")
    return text.replace(old, new, 1)


def patch_app() -> None:
    text = APP.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "private enum class Screen {\n    HOME,\n    IRANIAN_NAMES,",
        "private enum class Screen {\n    HOME,\n    ALL_NAMES,\n    IRANIAN_NAMES,",
        "Screen enum",
    )

    text = replace_once(
        text,
        '    Screen.HOME -> "نام‌نامه ایران"\n    Screen.IRANIAN_NAMES -> "اسامی اصیل ایرانی"',
        '    Screen.HOME -> "نام‌نامه ایران"\n    Screen.ALL_NAMES -> "همه نام‌ها"\n    Screen.IRANIAN_NAMES -> "اسامی اصیل ایرانی"',
        "screen title",
    )

    text = replace_once(
        text,
        '            item { DrawerItem("خانه", Icons.Rounded.Home, Screen.HOME, currentScreen, onNavigate) }\n            item { DrawerItem("اسامی اصیل ایرانی", Icons.Rounded.Book, Screen.IRANIAN_NAMES, currentScreen, onNavigate) }',
        '            item { DrawerItem("خانه", Icons.Rounded.Home, Screen.HOME, currentScreen, onNavigate) }\n            item { DrawerItem("همه نام‌ها", Icons.Rounded.People, Screen.ALL_NAMES, currentScreen, onNavigate) }\n            item { DrawerItem("اسامی اصیل ایرانی", Icons.Rounded.Book, Screen.IRANIAN_NAMES, currentScreen, onNavigate) }',
        "drawer all names item",
    )

    all_names_branch = '''                    Screen.ALL_NAMES -> NamesScreen(\n                        title = "همه نام‌ها",\n                        source = repository.names,\n                        favorites = favoriteNames,\n                        onFavorite = { name ->\n                            if (name in favoriteNames) favoriteNames.remove(name) else favoriteNames.add(name)\n                            persistFavorites()\n                        },\n                        onNameClick = ::openName,\n                    )\n\n'''
    if "Screen.ALL_NAMES -> NamesScreen" not in text:
        marker = "                    Screen.IRANIAN_NAMES -> NamesScreen("
        if marker not in text:
            raise RuntimeError("Could not patch All Names branch")
        text = text.replace(marker, all_names_branch + marker, 1)

    quick_chip = '''                    item {\n                        AssistChip(\n                            onClick = { onNavigate(Screen.ALL_NAMES) },\n                            label = { Text("همه نام‌ها") },\n                            leadingIcon = { Icon(Icons.Rounded.People, contentDescription = null) },\n                        )\n                    }\n'''
    if "onNavigate(Screen.ALL_NAMES)" not in text:
        marker = '''                    item {\n                        AssistChip(\n                            onClick = { onNavigate(Screen.IRANIAN_NAMES) },'''
        if marker not in text:
            raise RuntimeError("Could not patch All Names quick chip")
        text = text.replace(marker, quick_chip + marker, 1)

    APP.write_text(text, encoding="utf-8")


def patch_versions() -> None:
    text = BUILD.read_text(encoding="utf-8")
    text = text.replace("versionCode = 1", "versionCode = 2", 1)
    text = text.replace('versionName = "1.0.0"', 'versionName = "1.0.1"', 1)
    BUILD.write_text(text, encoding="utf-8")

    text = VERSION.read_text(encoding="utf-8")
    text = text.replace("versionName=1.0.0", "versionName=1.0.1", 1)
    text = text.replace("versionCode=1", "versionCode=2", 1)
    VERSION.write_text(text, encoding="utf-8")

    if README.exists():
        text = README.read_text(encoding="utf-8")
        text = text.replace("نسخه 1.0.0", "نسخه 1.0.1", 1)
        if "همه نام‌ها" not in text:
            text += "\n\n## اصلاح 1.0.1\n\n- فهرست کامل نام‌ها مستقیماً داخل سورس نگهداری می‌شود.\n- صفحه «همه نام‌ها» برای مرور کل دیتاست اضافه شده است.\n- دسته‌های فرهنگی فقط نام‌هایی را نمایش می‌دهند که انتساب فرهنگی آن‌ها بررسی شده است.\n"
        README.write_text(text, encoding="utf-8")

    if PROJECT_INFO.exists():
        text = PROJECT_INFO.read_text(encoding="utf-8")
        text = text.replace("Version: `1.0.0` (`versionCode=1`)", "Version: `1.0.1` (`versionCode=2`)", 1)
        PROJECT_INFO.write_text(text, encoding="utf-8")


def main() -> None:
    patch_app()
    patch_versions()
    print("Applied App-NameDic dataset visibility fix (v1.0.1)")


if __name__ == "__main__":
    main()
