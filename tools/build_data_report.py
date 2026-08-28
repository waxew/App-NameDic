#!/usr/bin/env python3
"""Write a deterministic coverage report for the bundled name corpus."""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
DATA_PATH = ASSETS / "names_base.json"
REPORT_PATH = ASSETS / "data_build_info.json"


def main() -> None:
    data = json.loads(DATA_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise RuntimeError("names_base.json must be an object")

    records = [item for item in data.values() if isinstance(item, dict)]
    genders = Counter(str(item.get("gender", "UNKNOWN")) for item in records)
    sources: Counter[str] = Counter()
    cultures: Counter[str] = Counter()

    for item in records:
        for source in item.get("sourceIds", []):
            if source:
                sources[str(source)] += 1
        for culture in item.get("usageCultureIds", []):
            if culture:
                cultures[str(culture)] += 1

    report = {
        "totalNames": len(records),
        "genderCounts": dict(sorted(genders.items())),
        "withLatin": sum(bool(item.get("latin")) for item in records),
        "withMultipleLatinVariants": sum(
            len(item.get("latinVariants", [])) > 1 for item in records
        ),
        "withMeaning": sum(bool(item.get("meaning")) for item in records),
        "withOrigin": sum(bool(item.get("origin")) for item in records),
        "withPronunciation": sum(bool(item.get("pronunciation")) for item in records),
        "withCultureClassificationBeyondGeneral": sum(
            any(c != "iran_general" for c in item.get("usageCultureIds", []))
            for item in records
        ),
        "multiSourceNames": sum(len(item.get("sourceIds", [])) >= 2 for item in records),
        "genderConflictNames": sum(bool(item.get("genderConflict")) for item in records),
        "sourceCoverage": dict(sorted(sources.items())),
        "cultureCoverage": dict(sorted(cultures.items())),
    }
    REPORT_PATH.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
