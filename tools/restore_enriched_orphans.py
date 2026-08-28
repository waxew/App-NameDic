#!/usr/bin/env python3
"""Restore enrichment-only records after rebuilding the list-source corpus.

A name can exist only because Wikidata/Wiktionary contributed it. Rebuilding the
base corpus from list datasets must not silently delete such records. CI takes a
snapshot before sync_names.py and this script restores previous enrichment-only
records that are still absent after the merge.
"""
from __future__ import annotations

import json
import pathlib
import sys
from typing import Any

ROOT = pathlib.Path(__file__).resolve().parents[1]
CURRENT_PATH = ROOT / "app/src/main/assets/names_base.json"
ENRICHMENT_SOURCES = {"wikidata_cc0", "wiktionary_kaikki", "sahim_official"}


def normalize(value: str) -> str:
    return (
        value.strip().lower()
        .replace("ي", "ی")
        .replace("ى", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def load(path: pathlib.Path) -> dict[str, dict[str, Any]]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        raise RuntimeError(f"Expected object JSON: {path}")
    return {str(k): v for k, v in raw.items() if isinstance(v, dict)}


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: restore_enriched_orphans.py <previous-names-base.json>")

    previous_path = pathlib.Path(sys.argv[1])
    if not previous_path.exists():
        print("No previous corpus snapshot; nothing to restore")
        return

    previous = load(previous_path)
    current = load(CURRENT_PATH)
    current_normalized = {
        normalize(str(item.get("name", key))): key for key, item in current.items()
    }

    restored = 0
    for key, item in previous.items():
        name = str(item.get("name", key)).strip()
        if not name or normalize(name) in current_normalized:
            continue
        sources = {str(value) for value in item.get("sourceIds", [])}
        if not (sources & ENRICHMENT_SOURCES):
            continue
        item["usageCultureIds"] = list(dict.fromkeys([
            "iran_general", *map(str, item.get("usageCultureIds", []))
        ]))
        current[name] = item
        current_normalized[normalize(name)] = name
        restored += 1

    CURRENT_PATH.write_text(
        json.dumps(current, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Restored {restored:,} enrichment-only records from previous corpus")


if __name__ == "__main__":
    main()
