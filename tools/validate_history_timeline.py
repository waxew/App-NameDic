#!/usr/bin/env python3
"""Validate the offline Iran history timeline before Android compilation.

The validator is intentionally strict about structure, stable ids and source URLs.
It does not try to judge historical interpretation; factual review remains a content
maintenance task, while this script prevents malformed records from shipping.
"""

from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
DATA_FILE = ROOT / "app" / "src" / "main" / "assets" / "history_timeline.json"
REQUIRED = {
    "id",
    "titleFa",
    "yearsFa",
    "eraFa",
    "summaryFa",
    "startSort",
    "endSort",
    "sourceLabel",
    "sourceUrl",
}


def main() -> None:
    data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    if not isinstance(data, list) or not data:
        raise SystemExit("history_timeline.json must be a non-empty JSON array")

    ids: set[str] = set()
    errors: list[str] = []

    for index, item in enumerate(data):
        if not isinstance(item, dict):
            errors.append(f"row {index}: expected object")
            continue

        missing = REQUIRED - item.keys()
        if missing:
            errors.append(f"row {index}: missing {sorted(missing)}")
            continue

        item_id = str(item["id"]).strip()
        if not item_id:
            errors.append(f"row {index}: empty id")
        elif item_id in ids:
            errors.append(f"row {index}: duplicate id {item_id}")
        ids.add(item_id)

        for key in ("titleFa", "yearsFa", "eraFa", "summaryFa", "sourceLabel"):
            if not str(item[key]).strip():
                errors.append(f"{item_id or index}: empty {key}")

        start = item["startSort"]
        end = item["endSort"]
        if not isinstance(start, int) or not isinstance(end, int):
            errors.append(f"{item_id or index}: startSort/endSort must be integers")
        elif start > end:
            errors.append(f"{item_id or index}: startSort must be <= endSort")

        parsed = urlparse(str(item["sourceUrl"]))
        if parsed.scheme != "https" or not parsed.netloc:
            errors.append(f"{item_id or index}: sourceUrl must be an absolute HTTPS URL")

    if len(data) < 8:
        errors.append("timeline should contain at least 8 substantial periods")

    if errors:
        raise SystemExit("History timeline validation failed:\n- " + "\n- ".join(errors))

    print(f"Validated {len(data)} history timeline records with unique ids and HTTPS sources.")


if __name__ == "__main__":
    main()
