#!/usr/bin/env python3
"""Validate all offline historical-figures datasets used by the Android app.

This validator intentionally checks structure rather than historical truth. Historical
claims are reviewed when records are curated; CI guarantees that broken/duplicate
records, empty summaries, or malformed source links cannot silently enter an APK.
"""

from __future__ import annotations

import json
from pathlib import Path
from urllib.parse import urlparse

DATA_FILES = (
    Path("app/src/main/assets/historical_figures.json"),
    Path("app/src/main/assets/historical_figures_extra.json"),
)
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


def load_records() -> list[tuple[Path, dict]]:
    """Read every declared content pack and preserve its file for useful errors."""
    loaded: list[tuple[Path, dict]] = []
    for data_file in DATA_FILES:
        if not data_file.exists():
            fail(f"missing file: {data_file}")
        try:
            records = json.loads(data_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            fail(f"invalid JSON in {data_file}: {exc}")
        if not isinstance(records, list) or not records:
            fail(f"{data_file}: top-level JSON must be a non-empty array")
        for record in records:
            loaded.append((data_file, record))
    return loaded


def main() -> None:
    records = load_records()
    seen_ids: dict[str, Path] = {}
    seen_names: dict[str, Path] = {}

    for index, (data_file, record) in enumerate(records):
        if not isinstance(record, dict):
            fail(f"{data_file}: record #{index + 1} is not an object")

        for field in REQUIRED_TEXT_FIELDS:
            value = record.get(field)
            if not isinstance(value, str) or not value.strip():
                fail(f"{data_file}: record #{index + 1} has empty/non-string field {field!r}")

        record_id = record["id"].strip()
        name_fa = record["nameFa"].strip()
        if record_id in seen_ids:
            fail(f"duplicate id across packs: {record_id} ({seen_ids[record_id]} and {data_file})")
        if name_fa in seen_names:
            fail(f"duplicate Persian name across packs: {name_fa} ({seen_names[name_fa]} and {data_file})")
        seen_ids[record_id] = data_file
        seen_names[name_fa] = data_file

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

    categories = sorted({record["categoryFa"] for _, record in records})
    print(
        f"[history-data] OK: {len(records)} figures across {len(DATA_FILES)} packs, "
        f"{len(categories)} categories, all ids and source URLs valid"
    )


if __name__ == "__main__":
    main()
