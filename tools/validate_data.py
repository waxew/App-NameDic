#!/usr/bin/env python3
"""Structural validation for bundled App-NameDic JSON data.

The bundled input may contain spelling variants that normalize to the same name
(e.g. spaced vs unspaced compound names). The multi-source aggregator is the
stage responsible for deduplication, so structural validation must not reject
those pre-merge variants.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
VALID_GENDERS = {"MALE", "FEMALE", "UNISEX", "UNKNOWN"}
VALID_STATUSES = {"VERIFIED", "CURATED", "BASE_ONLY", "NEEDS_REVIEW"}


def assert_string_list(value: Any, label: str) -> None:
    assert isinstance(value, list), f"{label} must be a list"
    assert all(isinstance(item, str) and item.strip() for item in value), (
        f"{label} must contain only non-empty strings"
    )


def assert_optional_text(item: dict[str, Any], field: str, name: str) -> None:
    if field not in item:
        return
    value = item[field]
    assert isinstance(value, str), f"{name}.{field} must be a string"
    assert value.strip(), f"{name}.{field} must not be blank when present"
    assert len(value) <= 2000, f"{name}.{field} is unexpectedly long"


def validate_curated() -> int:
    data = json.loads((ASSETS / "curated_names.json").read_text(encoding="utf-8"))
    assert isinstance(data, list)
    seen: set[str] = set()
    for index, item in enumerate(data):
        assert isinstance(item, dict), f"curated item {index} must be an object"
        name = str(item.get("name", "")).strip()
        assert name, f"empty curated name at index {index}"
        assert name not in seen, f"duplicate curated name: {name}"
        seen.add(name)
        assert item.get("gender", "UNKNOWN") in VALID_GENDERS
        assert item.get("verificationStatus", "NEEDS_REVIEW") in VALID_STATUSES
        assert_string_list(item.get("usageCultureIds", []), f"{name}.usageCultureIds")
        assert_string_list(item.get("tags", []), f"{name}.tags")
        assert_string_list(item.get("sourceIds", []), f"{name}.sourceIds")
        assert_string_list(item.get("latinVariants", []), f"{name}.latinVariants")
        assert_optional_text(item, "lexicalMeaningFa", name)
        assert_optional_text(item, "lexicalAntonymsFa", name)
    return len(data)


def validate_base() -> int:
    data = json.loads((ASSETS / "names_base.json").read_text(encoding="utf-8"))
    assert isinstance(data, dict)
    for key, item in data.items():
        assert str(key).strip() and isinstance(item, dict)
        name = str(item.get("name", key)).strip()
        assert name, f"empty base name for key {key!r}"
        assert item.get("gender", "UNKNOWN") in VALID_GENDERS

        for field in (
            "usageCultureIds",
            "classificationSourceIds",
            "sourceIds",
            "latinVariants",
            "tags",
            "wikidataIds",
        ):
            if field in item:
                assert_string_list(item[field], f"{name}.{field}")

        assert_optional_text(item, "lexicalMeaningFa", name)
        assert_optional_text(item, "lexicalAntonymsFa", name)

        if "sourceCount" in item:
            assert isinstance(item["sourceCount"], int) and item["sourceCount"] >= 0
            if "sourceIds" in item:
                assert item["sourceCount"] == len(set(item["sourceIds"])), (
                    f"{name}.sourceCount does not match sourceIds"
                )
        if "genderConflict" in item:
            assert isinstance(item["genderConflict"], bool)

    return len(data)


def validate_build_info() -> None:
    path = ASSETS / "data_build_info.json"
    if not path.exists():
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    assert isinstance(data, dict)
    assert isinstance(data.get("totalNames", 0), int)
    assert isinstance(data.get("sourceCoverage", {}), dict)
    assert isinstance(data.get("cultureCoverage", {}), dict)
    if "withLexicalMeaningFa" in data:
        assert isinstance(data["withLexicalMeaningFa"], int)
    if "withLexicalAntonymsFa" in data:
        assert isinstance(data["withLexicalAntonymsFa"], int)


def main() -> None:
    curated_count = validate_curated()
    base_count = validate_base()
    validate_build_info()
    print(f"OK: {curated_count:,} curated entries + {base_count:,} base entries")


if __name__ == "__main__":
    main()
