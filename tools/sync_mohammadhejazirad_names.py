#!/usr/bin/env python3
"""Merge the MIT-licensed mohammadhejazirad/persian-names gender corpus.

The upstream generated TypeScript file contains pipe-separated MALE_NAMES,
FEMALE_NAMES and UNISEX_NAMES constants. We parse those text constants only;
no upstream code is executed.
"""
from __future__ import annotations

import json
import pathlib
import re
import urllib.request
from typing import Any

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "app/src/main/assets/names_base.json"
SOURCE_ID = "mohammadhejazirad_persian_names"
SOURCE_URL = "https://raw.githubusercontent.com/mohammadhejazirad/persian-names/main/src/generated-data.ts"
PERSIAN_RE = re.compile(r"[\u0600-\u06ff]")
CONST_RE = re.compile(
    r'export const (MALE_NAMES|FEMALE_NAMES|UNISEX_NAMES): string = ("(?:\\.|[^"\\])*");'
)
GENDER_BY_CONST = {
    "MALE_NAMES": "MALE",
    "FEMALE_NAMES": "FEMALE",
    "UNISEX_NAMES": "UNISEX",
}


def fetch_text(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "App-NameDic-data-sync/1.1 (+https://github.com/waxew/App-NameDic)"},
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read().decode("utf-8-sig")


def normalize(value: str) -> str:
    return (
        value.strip().lower()
        .replace("ي", "ی")
        .replace("ى", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def clean_name(value: Any) -> str:
    return re.sub(r"\s+", " ", str(value or "").strip())


def valid_name(name: str) -> bool:
    return bool(name and PERSIAN_RE.search(name))


def uniq(values: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        value = str(value or "").strip()
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def apply_gender(record: dict[str, Any], incoming: str) -> None:
    current = str(record.get("gender", "UNKNOWN")).upper()
    if incoming == "UNISEX":
        record["gender"] = "UNISEX"
        return
    if current == "UNKNOWN":
        record["gender"] = incoming
        return
    if current in {"MALE", "FEMALE"} and current != incoming:
        record["gender"] = "UNISEX"
        record["genderConflict"] = True


def main() -> None:
    data = json.loads(DATA_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise RuntimeError("names_base.json must be an object")

    index: dict[str, dict[str, Any]] = {}
    for key, item in data.items():
        if not isinstance(item, dict):
            continue
        name = clean_name(item.get("name", key))
        if name:
            index[normalize(name)] = item

    text = fetch_text(SOURCE_URL)
    matches = CONST_RE.findall(text)
    if len(matches) != 3:
        raise RuntimeError(f"Expected three generated name constants, found {len(matches)}")

    added = 0
    matched_existing = 0
    processed = 0

    for const_name, encoded_literal in matches:
        gender = GENDER_BY_CONST[const_name]
        encoded = json.loads(encoded_literal)
        for raw_name in encoded.split("|"):
            name = clean_name(raw_name)
            if not valid_name(name):
                continue
            processed += 1
            key = normalize(name)
            record = index.get(key)
            if record is None:
                record = {
                    "name": name,
                    "gender": gender,
                    "latin": "",
                    "latinVariants": [],
                    "sourceIds": [SOURCE_ID],
                    "sourceCount": 1,
                    "usageCultureIds": ["iran_general"],
                    "classificationSourceIds": [],
                }
                data[name] = record
                index[key] = record
                added += 1
            else:
                matched_existing += 1
                apply_gender(record, gender)
                record["sourceIds"] = uniq([*record.get("sourceIds", []), SOURCE_ID])
                record["sourceCount"] = len(record["sourceIds"])
                record["usageCultureIds"] = uniq(
                    ["iran_general", *record.get("usageCultureIds", [])]
                )

    DATA_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        f"mohammadhejazirad/persian-names: processed {processed:,}; "
        f"matched {matched_existing:,}; added {added:,} new names"
    )


if __name__ == "__main__":
    main()
