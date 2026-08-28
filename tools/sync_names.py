#!/usr/bin/env python3
"""همگام‌سازی دیتاست عمومی نام‌های فارسی/ایرانی. خود برنامه در زمان اجرا آفلاین است."""
from __future__ import annotations
import json
import pathlib
import urllib.request

SOURCE_URL = "https://raw.githubusercontent.com/nabidam/persian-names/main/names.json"
ROOT = pathlib.Path(__file__).resolve().parents[1]
DESTINATION = ROOT / "app" / "src" / "main" / "assets" / "names_base.json"

def main() -> None:
    with urllib.request.urlopen(SOURCE_URL, timeout=30) as response:
        raw = response.read().decode("utf-8")
    data = json.loads(raw)
    if not isinstance(data, dict) or not data:
        raise RuntimeError("Downloaded names dataset has an unexpected format.")
    DESTINATION.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Synced {len(data):,} base names -> {DESTINATION}")

if __name__ == "__main__":
    main()
