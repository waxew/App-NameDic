#!/usr/bin/env python3
"""Restore enrichment metadata after rebuilding the list-source corpus.

A name can exist only because an enrichment source contributed it, or a normal
list-source name can carry expensive enrichment fields collected on a previous
run. Rebuilding the base list must not silently erase either case when one
external enrichment endpoint is temporarily unavailable.
"""
from __future__ import annotations

import json
import pathlib
import sys
from typing import Any

ROOT = pathlib.Path(__file__).resolve().parents[1]
CURRENT_PATH = ROOT / "app/src/main/assets/names_base.json"
ENRICHMENT_SOURCES = {
    "wikidata_cc0",
    "wiktionary_kaikki",
    "maani_dehkhoda_lexicon",
    "sahim_official",
}
PRESERVED_FIELDS = (
    "meaning",
    "origin",
    "pronunciation",
    "lexicalMeaningFa",
    "lexicalAntonymsFa",
    "notes",
    "wikidataIds",
    "wiktionaryPage",
)


def normalize(value: str) -> str:
    return (
        value.strip().lower()
        .replace("ي", "ی")
        .replace("ى", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def uniq(values: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for raw in values:
        value = str(raw or "").strip()
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def load(path: pathlib.Path) -> dict[str, dict[str, Any]]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise RuntimeError(f"Expected object JSON: {path}")
    return {str(k): v for k, v in raw.items() if isinstance(v, dict)}


def merge_existing(current_item: dict[str, Any], previous_item: dict[str, Any]) -> bool:
    previous_sources = uniq(list(map(str, previous_item.get("sourceIds", []))))
    enrichment_sources = [source for source in previous_sources if source in ENRICHMENT_SOURCES]
    changed = False

    for field in PRESERVED_FIELDS:
        previous_value = previous_item.get(field)
        current_value = current_item.get(field)
        if previous_value not in (None, "", [], {}) and current_value in (None, "", [], {}):
            current_item[field] = previous_value
            changed = True

    for list_field in ("usageCultureIds", "classificationSourceIds", "latinVariants", "tags"):
        previous_values = list(map(str, previous_item.get(list_field, [])))
        current_values = list(map(str, current_item.get(list_field, [])))
        merged = uniq([*current_values, *previous_values])
        if merged != current_values:
            current_item[list_field] = merged
            changed = True

    if enrichment_sources:
        current_sources = uniq(list(map(str, current_item.get("sourceIds", []))))
        merged_sources = uniq([*current_sources, *enrichment_sources])
        if merged_sources != current_sources:
            current_item["sourceIds"] = sorted(merged_sources)
            changed = True

    if changed:
        current_item["usageCultureIds"] = uniq(
            ["iran_general", *map(str, current_item.get("usageCultureIds", []))]
        )
        current_item["sourceCount"] = len(set(map(str, current_item.get("sourceIds", []))))
    return changed


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: restore_enriched_orphans.py <previous-names-base.json>")

    previous_path = pathlib.Path(sys.argv[1])
    if not previous_path.exists():
        print("No previous corpus snapshot; nothing to restore")
        return

    previous = load(previous_path)
    current = load(CURRENT_PATH)
    current_by_normalized = {
        normalize(str(item.get("name", key))): (key, item)
        for key, item in current.items()
    }

    restored_orphans = 0
    merged_existing = 0

    for key, previous_item in previous.items():
        name = str(previous_item.get("name", key)).strip()
        if not name:
            continue
        normalized = normalize(name)
        existing = current_by_normalized.get(normalized)
        if existing is not None:
            _, current_item = existing
            if merge_existing(current_item, previous_item):
                merged_existing += 1
            continue

        sources = {str(value) for value in previous_item.get("sourceIds", [])}
        if not (sources & ENRICHMENT_SOURCES):
            continue
        previous_item["usageCultureIds"] = uniq([
            "iran_general", *map(str, previous_item.get("usageCultureIds", []))
        ])
        previous_item["sourceIds"] = sorted(uniq(list(map(str, previous_item.get("sourceIds", [])))))
        previous_item["sourceCount"] = len(previous_item["sourceIds"])
        current[name] = previous_item
        current_by_normalized[normalized] = (name, previous_item)
        restored_orphans += 1

    CURRENT_PATH.write_text(
        json.dumps(current, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        f"Restored {restored_orphans:,} enrichment-only records and merged "
        f"previous enrichment into {merged_existing:,} existing records"
    )


if __name__ == "__main__":
    main()
