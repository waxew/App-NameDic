#!/usr/bin/env python3
"""Best-effort enrichment from Kaikki/Wiktionary Persian name entries.

The postprocessed Persian JSONL is derived from Wiktionary and is dual-licensed
CC BY-SA / GFDL. Only structured facts useful to the name dictionary are kept:
romanization, IPA, gender categories, Persian-name classification and concise
etymological origin labels. Long Wiktionary prose/glosses are not copied.
"""
from __future__ import annotations

import json
import pathlib
import re
import urllib.parse
import urllib.request
from typing import Any, Iterable

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "app/src/main/assets/names_base.json"
KAIKKI_URL = "https://kaikki.org/dictionary/Persian/kaikki.org-dictionary-Persian.jsonl"

LANGUAGE_NAMES_FA = {
    "Middle Persian": "فارسی میانه",
    "Manichaean Middle Persian": "فارسی میانه مانوی",
    "Old Persian": "پارسی باستان",
    "Avestan": "اوستایی",
    "Persian": "فارسی",
    "Central Kurdish": "کردی مرکزی",
    "Kurdish": "کردی",
    "Azerbaijani": "ترکی آذربایجانی",
    "Turkish": "ترکی",
    "Arabic": "عربی",
    "Classical Arabic": "عربی کلاسیک",
    "Armenian": "ارمنی",
    "Hebrew": "عبری",
    "Greek": "یونانی",
    "Ancient Greek": "یونانی باستان",
    "Mongolian": "مغولی",
    "French": "فرانسوی",
    "English": "انگلیسی",
    "Russian": "روسی",
}

ORIGIN_PATTERNS = (
    re.compile(r"^Persian (?:female |male |unisex )?given names from (.+)$"),
    re.compile(r"^Persian terms (?:borrowed|derived|inherited) from (.+)$"),
)


def uniq(values: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for raw in values:
        value = str(raw or "").strip()
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def normalize(value: str) -> str:
    return (
        value.strip().lower()
        .replace("ي", "ی")
        .replace("ى", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def category_names(entry: dict[str, Any]) -> list[str]:
    result: list[str] = []

    def collect(values: Any) -> None:
        if not isinstance(values, list):
            return
        for value in values:
            if isinstance(value, str):
                result.append(value)
            elif isinstance(value, dict):
                name = str(value.get("name", "")).strip()
                if name:
                    result.append(name)

    collect(entry.get("categories"))
    for sense in entry.get("senses", []):
        if isinstance(sense, dict):
            collect(sense.get("categories"))
    return uniq(result)


def gender_from_categories(categories: list[str]) -> str:
    lowered = [c.lower() for c in categories]
    if any("unisex given names" in c for c in lowered):
        return "UNISEX"
    female = any("female given names" in c for c in lowered)
    male = any("male given names" in c for c in lowered)
    if female and male:
        return "UNISEX"
    if female:
        return "FEMALE"
    if male:
        return "MALE"
    return "UNKNOWN"


def origin_from_categories(categories: list[str]) -> str:
    for pattern in ORIGIN_PATTERNS:
        for category in categories:
            match = pattern.match(category)
            if match:
                raw = match.group(1).strip()
                return LANGUAGE_NAMES_FA.get(raw, raw)
    return ""


def romanizations(entry: dict[str, Any]) -> list[str]:
    values: list[str] = []
    for form in entry.get("forms", []):
        if not isinstance(form, dict):
            continue
        tags = {str(tag).lower() for tag in form.get("tags", [])}
        value = str(form.get("form", "")).strip()
        if value and "romanization" in tags:
            values.append(value)
    return uniq(values)


def select_ipa(entry: dict[str, Any]) -> str:
    candidates: list[tuple[int, str]] = []
    for sound in entry.get("sounds", []):
        if not isinstance(sound, dict):
            continue
        ipa = str(sound.get("ipa", "")).strip()
        if not ipa:
            continue
        tags = {str(tag).lower() for tag in sound.get("tags", [])}
        note = str(sound.get("note", "")).lower()
        score = 0
        if "iran" in tags or "iran" in note:
            score += 10
        if "formal" in tags:
            score += 3
        if "classical-persian" in tags:
            score -= 2
        candidates.append((score, ipa))
    if not candidates:
        return ""
    candidates.sort(key=lambda x: x[0], reverse=True)
    return candidates[0][1]


def make_page_url(word: str) -> str:
    if not word:
        return ""
    first = urllib.parse.quote(word[:1], safe="")
    first_two = urllib.parse.quote(word[:2], safe="")
    full = urllib.parse.quote(word, safe="")
    return f"https://kaikki.org/dictionary/Persian/meaning/{first}/{first_two}/{full}.html"


def enrich_entry(record: dict[str, Any], entry: dict[str, Any]) -> bool:
    categories = category_names(entry)
    if not any("Persian given names" in category for category in categories):
        return False

    record["sourceIds"] = uniq([*record.get("sourceIds", []), "wiktionary_kaikki"])
    record["classificationSourceIds"] = uniq(
        [*record.get("classificationSourceIds", []), "wiktionary_kaikki"]
    )
    record["usageCultureIds"] = uniq([*record.get("usageCultureIds", []), "persian"])

    wt_gender = gender_from_categories(categories)
    if record.get("gender", "UNKNOWN") == "UNKNOWN" and wt_gender != "UNKNOWN":
        record["gender"] = wt_gender

    variants = romanizations(entry)
    if variants:
        record["latinVariants"] = uniq([*record.get("latinVariants", []), *variants])
        if not record.get("latin"):
            record["latin"] = record["latinVariants"][0]

    ipa = select_ipa(entry)
    if ipa and not record.get("pronunciation"):
        record["pronunciation"] = ipa

    origin = origin_from_categories(categories)
    if origin and not record.get("origin"):
        record["origin"] = origin

    word = str(entry.get("word", "")).strip()
    if word:
        record["wiktionaryPage"] = make_page_url(word)

    record["sourceCount"] = len(uniq(record.get("sourceIds", [])))
    return True


def main() -> None:
    data = json.loads(DATA_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise RuntimeError("names_base.json must be an object")

    index: dict[str, dict[str, Any]] = {}
    for item in data.values():
        if isinstance(item, dict) and item.get("name"):
            index[normalize(str(item["name"]))] = item

    request = urllib.request.Request(
        KAIKKI_URL,
        headers={
            "User-Agent": "App-NameDic/1.1 (https://github.com/waxew/App-NameDic; Wiktionary enrichment)"
        },
    )

    matched = 0
    processed_name_entries = 0
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            for raw_line in response:
                try:
                    entry = json.loads(raw_line.decode("utf-8"))
                except Exception:
                    continue
                if not isinstance(entry, dict) or entry.get("lang_code") != "fa":
                    continue
                if entry.get("pos") != "name":
                    continue
                processed_name_entries += 1
                word = str(entry.get("word", "")).strip()
                record = index.get(normalize(word))
                if record is None:
                    continue
                if enrich_entry(record, entry):
                    matched += 1
    except Exception as exc:
        print(f"WARN Kaikki/Wiktionary enrichment skipped: {exc}")
        return

    DATA_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        f"Kaikki/Wiktionary: processed {processed_name_entries:,} Persian name entries; "
        f"matched {matched:,} local records"
    )


if __name__ == "__main__":
    main()
