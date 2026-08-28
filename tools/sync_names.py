#!/usr/bin/env python3
"""Build a deterministic, multi-source Iranian/Persian name corpus.

Only sources with explicit reusable licenses are imported automatically.
Meanings, etymologies and cultural labels are never guessed from a bare name.
Existing structured enrichments (e.g. Wikidata/Wiktionary) are preserved across
runs so a transient network failure does not erase previously collected data.
"""
from __future__ import annotations

import csv
import io
import json
import pathlib
import re
import urllib.request
from collections import Counter
from typing import Any, Iterable

ROOT = pathlib.Path(__file__).resolve().parents[1]
DESTINATION = ROOT / "app/src/main/assets/names_base.json"

SOURCES = {
    "nabidam_persian_names": {
        "url": "https://raw.githubusercontent.com/nabidam/persian-names/main/names.json",
        "license": "MIT",
    },
    "farbodbj_pngt": {
        "url": "https://raw.githubusercontent.com/farbodbj/persian-gender-by-name/github-master/persian-gender-by-name.csv",
        "license": "Apache-2.0",
    },
    "mehdi_iranian_names_female": {
        "url": "https://raw.githubusercontent.com/mehdi-haydari/iranianNames/master/female.js",
        "license": "MIT",
    },
    "mehdi_iranian_names_male": {
        "url": "https://raw.githubusercontent.com/mehdi-haydari/iranianNames/master/male.js",
        "license": "MIT",
    },
    "jadijadi_persianwords_boy": {
        "url": "https://raw.githubusercontent.com/jadijadi/persianwords/master/name_boy.txt",
        "license": "CC0-1.0",
    },
    "jadijadi_persianwords_girl": {
        "url": "https://raw.githubusercontent.com/jadijadi/persianwords/master/name_girl.txt",
        "license": "CC0-1.0",
    },
    "armanyazdi_persian_names_male": {
        "url": "https://raw.githubusercontent.com/armanyazdi/persian-names/main/persian_names/data/male_fa.txt",
        "license": "MIT",
    },
    "armanyazdi_persian_names_female": {
        "url": "https://raw.githubusercontent.com/armanyazdi/persian-names/main/persian_names/data/female_fa.txt",
        "license": "MIT",
    },
}

PERSIAN_RE = re.compile(r"[\u0600-\u06ff]")
CONFLICT_PREFIXES = ("<<<<<<<", "=======", ">>>>>>>")


def fetch_text(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "App-NameDic-data-sync/1.1 (+https://github.com/waxew/App-NameDic)"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8-sig")


def normalize(value: str) -> str:
    return (
        value.strip()
        .lower()
        .replace("ي", "ی")
        .replace("ى", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def clean_name(value: Any) -> str:
    name = str(value or "").strip()
    name = re.sub(r"\s+", " ", name)
    return name


def valid_name(name: str) -> bool:
    return bool(name and PERSIAN_RE.search(name) and not name.startswith(CONFLICT_PREFIXES))


def normalize_gender(value: Any) -> str:
    text = str(value or "").strip().lower()
    if text in {"m", "male", "boy", "man", "1"}:
        return "MALE"
    if text in {"f", "female", "girl", "woman", "2"}:
        return "FEMALE"
    if text in {"unisex", "both", "mf", "fm"}:
        return "UNISEX"
    return "UNKNOWN"


def good_latin(value: Any) -> str:
    text = str(value or "").strip()
    if not text or len(text) > 80 or PERSIAN_RE.search(text):
        return ""
    return re.sub(r"\s+", " ", text)


def uniq(values: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for raw in values:
        value = str(raw or "").strip()
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def load_previous() -> dict[str, dict[str, Any]]:
    if not DESTINATION.exists():
        return {}
    try:
        raw = json.loads(DESTINATION.read_text(encoding="utf-8"))
    except Exception:
        return {}
    if not isinstance(raw, dict):
        return {}
    result: dict[str, dict[str, Any]] = {}
    for key, item in raw.items():
        if not isinstance(item, dict):
            continue
        name = clean_name(item.get("name", key))
        if valid_name(name):
            result[normalize(name)] = dict(item)
    return result


class Corpus:
    def __init__(self, previous: dict[str, dict[str, Any]]) -> None:
        self.previous = previous
        self.records: dict[str, dict[str, Any]] = {}
        self.gender_votes: dict[str, Counter[str]] = {}

    def add(
        self,
        name: str,
        *,
        gender: str = "UNKNOWN",
        latin: str = "",
        source_id: str,
    ) -> None:
        name = clean_name(name)
        if not valid_name(name):
            return
        key = normalize(name)
        previous = self.previous.get(key, {})
        record = self.records.setdefault(
            key,
            {
                "name": clean_name(previous.get("name", name)) or name,
                "latinVariants": [],
                "sourceIds": [],
                "usageCultureIds": uniq(previous.get("usageCultureIds", [])),
                "classificationSourceIds": uniq(previous.get("classificationSourceIds", [])),
            },
        )

        if not record.get("name"):
            record["name"] = name

        gender_norm = normalize_gender(gender)
        if gender_norm != "UNKNOWN":
            self.gender_votes.setdefault(key, Counter())[gender_norm] += 1

        latin_value = good_latin(latin)
        if latin_value:
            record["latinVariants"] = uniq([*record.get("latinVariants", []), latin_value])

        record["sourceIds"] = uniq([*record.get("sourceIds", []), source_id])

        for field in (
            "meaning",
            "origin",
            "pronunciation",
            "notes",
            "wikidataIds",
            "wiktionaryPage",
        ):
            value = previous.get(field)
            if value not in (None, "", [], {}):
                record[field] = value

        if previous.get("tags"):
            record["tags"] = uniq(previous.get("tags", []))
        if previous.get("latin"):
            record["latinVariants"] = uniq([previous["latin"], *record.get("latinVariants", [])])

    def finalize(self) -> dict[str, dict[str, Any]]:
        output: dict[str, dict[str, Any]] = {}
        for key in sorted(self.records, key=lambda x: (x, self.records[x]["name"])):
            record = self.records[key]
            votes = self.gender_votes.get(key, Counter())
            genders = {g for g, count in votes.items() if count > 0}
            has_gender_conflict = {"MALE", "FEMALE"} <= genders
            if "UNISEX" in genders or has_gender_conflict:
                gender = "UNISEX"
            elif "MALE" in genders:
                gender = "MALE"
            elif "FEMALE" in genders:
                gender = "FEMALE"
            else:
                gender = normalize_gender(self.previous.get(key, {}).get("gender"))

            record["gender"] = gender
            if has_gender_conflict:
                record["genderConflict"] = True
            else:
                record.pop("genderConflict", None)
            record["latinVariants"] = uniq(record.get("latinVariants", []))
            record["latin"] = record["latinVariants"][0] if record["latinVariants"] else ""
            record["sourceIds"] = sorted(uniq(record.get("sourceIds", [])))
            record["sourceCount"] = len(record["sourceIds"])
            record["usageCultureIds"] = uniq(["iran_general", *record.get("usageCultureIds", [])])
            record["classificationSourceIds"] = sorted(
                uniq(record.get("classificationSourceIds", []))
            )
            if not record.get("meaning"):
                record.pop("meaning", None)
            if not record.get("origin"):
                record.pop("origin", None)
            if not record.get("pronunciation"):
                record.pop("pronunciation", None)
            if not record.get("notes"):
                record.pop("notes", None)
            if not record.get("tags"):
                record.pop("tags", None)
            if not record.get("wikidataIds"):
                record.pop("wikidataIds", None)
            if not record.get("wiktionaryPage"):
                record.pop("wiktionaryPage", None)
            output[record["name"]] = record
        return output


def ingest_nabidam(corpus: Corpus) -> None:
    source_id = "nabidam_persian_names"
    payload = json.loads(fetch_text(SOURCES[source_id]["url"]))
    if not isinstance(payload, dict):
        raise RuntimeError("nabidam dataset is not a JSON object")
    for key, item in payload.items():
        item = item if isinstance(item, dict) else {}
        corpus.add(
            clean_name(item.get("name", key)),
            gender=item.get("gender"),
            source_id=source_id,
        )


def ingest_farbodbj(corpus: Corpus) -> None:
    source_id = "farbodbj_pngt"
    text = fetch_text(SOURCES[source_id]["url"])
    for row in csv.DictReader(io.StringIO(text)):
        corpus.add(
            clean_name(row.get("name")),
            gender=row.get("gender"),
            latin=row.get("english_name", ""),
            source_id=source_id,
        )


def ingest_mehdi(corpus: Corpus, source_id: str) -> None:
    payload = json.loads(fetch_text(SOURCES[source_id]["url"]))
    if not isinstance(payload, list):
        raise RuntimeError(f"{source_id} is not a JSON array")
    for item in payload:
        if not isinstance(item, dict):
            continue
        corpus.add(
            clean_name(item.get("persian")),
            gender=item.get("sex"),
            latin=item.get("english", ""),
            source_id=source_id,
        )


def ingest_plain_names(corpus: Corpus, source_id: str, gender: str) -> None:
    text = fetch_text(SOURCES[source_id]["url"])
    for raw_line in text.splitlines():
        line = clean_name(raw_line)
        if not line or line.startswith(CONFLICT_PREFIXES):
            continue
        corpus.add(line, gender=gender, source_id=source_id)


def main() -> None:
    previous = load_previous()
    corpus = Corpus(previous)
    jobs = [
        ("nabidam_persian_names", lambda: ingest_nabidam(corpus)),
        ("farbodbj_pngt", lambda: ingest_farbodbj(corpus)),
        ("mehdi_iranian_names_female", lambda: ingest_mehdi(corpus, "mehdi_iranian_names_female")),
        ("mehdi_iranian_names_male", lambda: ingest_mehdi(corpus, "mehdi_iranian_names_male")),
        ("jadijadi_persianwords_boy", lambda: ingest_plain_names(corpus, "jadijadi_persianwords_boy", "MALE")),
        ("jadijadi_persianwords_girl", lambda: ingest_plain_names(corpus, "jadijadi_persianwords_girl", "FEMALE")),
        ("armanyazdi_persian_names_male", lambda: ingest_plain_names(corpus, "armanyazdi_persian_names_male", "MALE")),
        ("armanyazdi_persian_names_female", lambda: ingest_plain_names(corpus, "armanyazdi_persian_names_female", "FEMALE")),
    ]

    successes = 0
    failures: list[str] = []
    for source_id, job in jobs:
        try:
            job()
            successes += 1
            print(f"OK source: {source_id}")
        except Exception as exc:
            failures.append(f"{source_id}: {exc}")
            print(f"WARN source failed: {source_id}: {exc}")

    if not corpus.records:
        raise RuntimeError("No usable names were available from any source or previous corpus")

    output = corpus.finalize()
    DESTINATION.write_text(
        json.dumps(output, ensure_ascii=False, indent=2, sort_keys=False) + "\n",
        encoding="utf-8",
    )
    print(
        f"Built {len(output):,} unique names from {successes}/{len(jobs)} reachable sources"
    )
    if failures:
        print("Source warnings:")
        for failure in failures:
            print(f" - {failure}")


if __name__ == "__main__":
    main()
