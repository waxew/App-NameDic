#!/usr/bin/env python3
"""اعتبارسنجی ساختاری دیتاست‌های JSON قبل از build/release."""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
VALID_GENDERS = {"MALE", "FEMALE", "UNISEX", "UNKNOWN"}
VALID_STATUSES = {"VERIFIED", "CURATED", "BASE_ONLY", "NEEDS_REVIEW"}

def validate_curated() -> int:
    data = json.loads((ASSETS / "curated_names.json").read_text(encoding="utf-8"))
    assert isinstance(data, list)
    seen = set()
    for index, item in enumerate(data):
        assert item["name"].strip(), f"empty name at index {index}"
        assert item["name"] not in seen, f"duplicate curated name: {item['name']}"
        seen.add(item["name"])
        assert item.get("gender", "UNKNOWN") in VALID_GENDERS
        assert item.get("verificationStatus", "NEEDS_REVIEW") in VALID_STATUSES
        assert isinstance(item.get("usageCultureIds", []), list)
    return len(data)

def validate_base() -> int:
    data = json.loads((ASSETS / "names_base.json").read_text(encoding="utf-8"))
    assert isinstance(data, dict)
    for key, item in data.items():
        assert key.strip() and isinstance(item, dict)
        assert item.get("gender", "UNKNOWN") in VALID_GENDERS
    return len(data)

def main() -> None:
    print(f"OK: {validate_curated()} curated entries + {validate_base()} base entries")

if __name__ == "__main__":
    main()
