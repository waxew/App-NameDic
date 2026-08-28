#!/usr/bin/env python3
"""همگام‌سازی دیتاست عمومی نام‌ها و افزودن نوشتار لاتین از منابع دارای مجوز آزاد.

خود برنامه در زمان اجرا آفلاین است. این اسکریپت فقط هنگام توسعه/CI اجرا می‌شود.
هیچ معنی، ریشه یا دسته فرهنگی از روی نام حدس زده نمی‌شود.
"""
from __future__ import annotations

import json
import pathlib
import urllib.request

BASE_SOURCE_URL = "https://raw.githubusercontent.com/nabidam/persian-names/main/names.json"
LATIN_SOURCE_URLS = (
    "https://raw.githubusercontent.com/mehdi-haydari/iranianNames/master/female.js",
    "https://raw.githubusercontent.com/mehdi-haydari/iranianNames/master/male.js",
)
ROOT = pathlib.Path(__file__).resolve().parents[1]
DESTINATION = ROOT / "app" / "src" / "main" / "assets" / "names_base.json"


def fetch_text(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "App-NameDic-data-sync"})
    with urllib.request.urlopen(request, timeout=45) as response:
        return response.read().decode("utf-8-sig")


def normalize(value: str) -> str:
    return (
        value.strip()
        .lower()
        .replace("ي", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def load_latin_map() -> dict[str, str]:
    """نوشتار لاتین را از دیتاست MIT جداگانه می‌خواند.

    منبع خودش هشدار داده که بعضی transliterationها ممکن است دقیق نباشند؛
    بنابراین این فیلد فقط برای جستجو/نمایش کمکی استفاده می‌شود و ریشه‌شناسی نیست.
    """
    result: dict[str, str] = {}
    for url in LATIN_SOURCE_URLS:
        payload = json.loads(fetch_text(url))
        if not isinstance(payload, list):
            raise RuntimeError(f"Unexpected Latin source format: {url}")
        for item in payload:
            if not isinstance(item, dict):
                continue
            persian = str(item.get("persian", "")).strip()
            english = str(item.get("english", "")).strip()
            if persian and english:
                result.setdefault(normalize(persian), english)
    return result


def main() -> None:
    data = json.loads(fetch_text(BASE_SOURCE_URL))
    if not isinstance(data, dict) or not data:
        raise RuntimeError("Downloaded names dataset has an unexpected format.")

    latin_map = load_latin_map()
    latin_matches = 0

    for key, raw_item in data.items():
        item = raw_item if isinstance(raw_item, dict) else {}
        if item is not raw_item:
            data[key] = item
        name = str(item.get("name", key)).strip() or key
        latin = latin_map.get(normalize(name), "").strip()
        if latin:
            item["latin"] = latin
            latin_matches += 1

    DESTINATION.write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(
        f"Synced {len(data):,} base names -> {DESTINATION}; "
        f"Latin matched for {latin_matches:,} names"
    )


if __name__ == "__main__":
    main()
