#!/usr/bin/env python3
"""Validate the offline historical-figures dataset used by the Android app.

This validator intentionally checks structure rather than historical truth. Historical
claims are reviewed when records are curated; CI guarantees that broken/duplicate
records, empty summaries, or malformed source links cannot silently enter an APK.
"""

from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import urlparse

DATA_FILE = Path("app/src/main/assets/historical_figures.json")
REQUIRED_TEXT_FIELDS = (
    "id",
    "nameFa",
    "nameEn",
    "periodFa",
    "categoryFa",
    "roleFa",
    "yearsFa",
    "summaryFa",
    "sourceLabel",
    "sourceUrl",
)


def fail(message: str) -> None:
    """Terminate validation with a clear CI error."""
    raise SystemExit(f"[history-data] ERROR: {message}")


def main() -> None:
    if not DATA_FILE.exists():
        fail(f"missing file: {DATA_FILE}")

    try:
        records = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(f"invalid JSON: {exc}")

    if not isinstance(records, list) or not records:
        fail("top-level JSON must be a non-empty array")

    seen_ids: set[str] = set()
    seen_names: set[str] = set()

    for index, record in enumerate(records):
        if not isinstance(record, dict):
            fail(f"record #{index + 1} is not an object")

        for field in REQUIRED_TEXT_FIELDS:
            value = record.get(field)
            if not isinstance(value, str) or not value.strip():
                fail(f"record #{index + 1} has empty/non-string field {field!r}")

        record_id = record["id"].strip()
        name_fa = record["nameFa"].strip()
        if record_id in seen_ids:
            fail(f"duplicate id: {record_id}")
        if name_fa in seen_names:
            fail(f"duplicate Persian name: {name_fa}")
        seen_ids.add(record_id)
        seen_names.add(name_fa)

        highlights = record.get("highlightsFa")
        if not isinstance(highlights, list) or len(highlights) < 2:
            fail(f"{record_id}: highlightsFa must contain at least two items")
        if any(not isinstance(item, str) or not item.strip() for item in highlights):
            fail(f"{record_id}: highlightsFa contains an empty/non-string item")

        source = urlparse(record["sourceUrl"])
        if source.scheme != "https" or not source.netloc:
            fail(f"{record_id}: sourceUrl must be a valid HTTPS URL")

        if len(record["summaryFa"].strip()) < 80:
            fail(f"{record_id}: summaryFa is too short for a useful profile")

    categories = sorted({record["categoryFa"] for record in records})
    print(
        f"[history-data] OK: {len(records)} figures, "
        f"{len(categories)} categories, all ids and source URLs valid"
    )


if __name__ == "__main__":
    main()
