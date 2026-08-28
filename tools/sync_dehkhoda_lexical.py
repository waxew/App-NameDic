#!/usr/bin/env python3
"""Enrich names with Persian lexical synonyms/antonyms from Maani/Dehkhoda-Lexicon.

The dataset is CC BY-SA 4.0 and contains Persian headwords followed by Persian
synonyms and (when present) antonyms. These values are intentionally stored as
`lexicalMeaningFa` / `lexicalAntonymsFa`, NOT as the definitive meaning of a
personal name. A name may merely be homographic with an ordinary Persian word.

No AI translation is performed in this pipeline. We only preserve concise
Persian lexical relations explicitly present in the licensed dataset.
"""
from __future__ import annotations

import json
import pathlib
import re
import urllib.request
from typing import Any, Iterable

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "app/src/main/assets/names_base.json"
SOURCE_ID = "maani_dehkhoda_lexicon"
SOURCE_URL = (
    "https://huggingface.co/datasets/Maani/Dehkhoda-Lexicon/resolve/main/"
    "Dehkhoda_Lexicon.json?download=true"
)
PERSIAN_RE = re.compile(r"[\u0600-\u06ff]")
SPLIT_RE = re.compile(r"[،,؛;]+")


def fetch_text(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "App-NameDic/1.2 (+https://github.com/waxew/App-NameDic; lexical enrichment)"
        },
    )
    with urllib.request.urlopen(request, timeout=180) as response:
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


def uniq(values: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for raw in values:
        value = re.sub(r"\s+", " ", str(raw or "").strip())
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def clean_terms(text: str, headword: str) -> list[str]:
    result: list[str] = []
    head_key = normalize(headword)
    for piece in SPLIT_RE.split(text):
        term = re.sub(r"\s+", " ", piece.strip(" .ـ-–—\t\r\n"))
        if not term or len(term) > 80 or not PERSIAN_RE.search(term):
            continue
        if normalize(term) == head_key:
            continue
        result.append(term)
    return uniq(result)[:12]


def records_from_payload(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    if isinstance(payload, dict):
        for key in ("train", "data", "items", "records"):
            value = payload.get(key)
            if isinstance(value, list):
                return [item for item in value if isinstance(item, dict)]
    raise RuntimeError("Unexpected Dehkhoda-Lexicon JSON structure")


def parse_input(raw_input: str) -> tuple[str, list[str], list[str]] | None:
    text = str(raw_input or "").strip()
    if ":" not in text:
        return None
    headword, relations = text.split(":", 1)
    headword = re.sub(r"\s+", " ", headword.strip())
    if not headword or not PERSIAN_RE.search(headword):
        return None

    synonyms_text, separator, antonyms_text = relations.partition("&")
    synonyms = clean_terms(synonyms_text, headword)
    antonyms = clean_terms(antonyms_text, headword) if separator else []
    if not synonyms and not antonyms:
        return None
    return headword, synonyms, antonyms


def main() -> None:
    data = json.loads(DATA_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise RuntimeError("names_base.json must be an object")

    index: dict[str, dict[str, Any]] = {}
    for key, item in data.items():
        if not isinstance(item, dict):
            continue
        name = str(item.get("name", key)).strip()
        if name:
            index[normalize(name)] = item

    try:
        payload = json.loads(fetch_text(SOURCE_URL))
        source_records = records_from_payload(payload)
    except Exception as exc:
        print(f"WARN Dehkhoda lexical enrichment skipped: {exc}")
        return

    parsed = 0
    matched = 0
    with_synonyms = 0
    with_antonyms = 0

    for source_record in source_records:
        parsed_entry = parse_input(str(source_record.get("input", "")))
        if parsed_entry is None:
            continue
        parsed += 1
        headword, synonyms, antonyms = parsed_entry
        record = index.get(normalize(headword))
        if record is None:
            continue

        matched += 1
        if synonyms:
            record["lexicalMeaningFa"] = "، ".join(synonyms)
            with_synonyms += 1
        if antonyms:
            record["lexicalAntonymsFa"] = "، ".join(antonyms)
            with_antonyms += 1

        record["sourceIds"] = sorted(
            uniq([*record.get("sourceIds", []), SOURCE_ID])
        )
        record["sourceCount"] = len(record["sourceIds"])

    DATA_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        "Maani/Dehkhoda lexical enrichment: "
        f"{len(source_records):,} source rows; {parsed:,} parsed; "
        f"{matched:,} matched names; {with_synonyms:,} with Persian synonyms; "
        f"{with_antonyms:,} with antonyms"
    )


if __name__ == "__main__":
    main()
