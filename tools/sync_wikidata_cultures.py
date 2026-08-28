#!/usr/bin/env python3
"""Enrich cultural/language classification from Wikidata structured data (CC0).

P407 ("language of work or name") is used only as a language association.
It is deliberately NOT copied into NameEntry.origin because language
association is not proof of etymological origin.
"""
from __future__ import annotations

import json
import pathlib
import re
import time
import urllib.parse
import urllib.request
from typing import Any

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "app/src/main/assets/names_base.json"
ENDPOINT = "https://query.wikidata.org/sparql"

LANGUAGES = {
    "persian": ("Q9168", "Persian"),
    "azerbaijani": ("Q9292", "Azerbaijani"),
    "kurdish": ("Q36368", "Kurdish"),
    "gilaki": ("Q33657", "Gilaki"),
    "mazandarani": ("Q13356", "Mazanderani"),
    "luri": ("Q4701277", "Luri"),
    "balochi": ("Q33049", "Balochi"),
    "talysh": ("Q34318", "Talysh"),
    "tati": ("Q34165", "Tati"),
    "semnani": ("Q14531212", "Semnani"),
    "turkmen": ("Q9267", "Turkmen"),
    "qashqai": ("Q13192", "Qashqai"),
}

CLASS_GENDER = {
    "Q12308941": "MALE",
    "Q11879590": "FEMALE",
    "Q3409032": "UNISEX",
    "Q202444": "UNKNOWN",
}
PERSIAN_RE = re.compile(r"[\u0600-\u06ff]")


def uniq(values: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for raw in values:
        value = str(raw or "").strip()
        if value and value not in seen:
            seen.add(value)
            result.append(value)
    return result


def normalize_fa(value: str) -> str:
    return (
        value.strip().lower()
        .replace("ي", "ی")
        .replace("ى", "ی")
        .replace("ك", "ک")
        .replace("‌", "")
        .replace(" ", "")
    )


def normalize_latin(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", value.lower())


def entity_id(uri: str) -> str:
    return uri.rsplit("/", 1)[-1]


def fetch_bindings(language_qid: str) -> list[dict[str, Any]]:
    query = f"""
SELECT ?item ?class ?faLabel ?enLabel WHERE {{
  ?item wdt:P31 ?class ;
        wdt:P407 wd:{language_qid} .
  VALUES ?class {{ wd:Q12308941 wd:Q11879590 wd:Q3409032 wd:Q202444 }}
  OPTIONAL {{ ?item rdfs:label ?faLabel . FILTER(LANG(?faLabel) = "fa") }}
  OPTIONAL {{ ?item rdfs:label ?enLabel . FILTER(LANG(?enLabel) = "en") }}
}}
LIMIT 10000
""".strip()
    url = ENDPOINT + "?" + urllib.parse.urlencode({"query": query, "format": "json"})
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/sparql-results+json",
            "User-Agent": "App-NameDic/1.1 (https://github.com/waxew/App-NameDic; data enrichment)",
        },
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        payload = json.loads(response.read().decode("utf-8"))
    return payload.get("results", {}).get("bindings", [])


def main() -> None:
    data = json.loads(DATA_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise RuntimeError("names_base.json must be an object")

    by_fa: dict[str, list[dict[str, Any]]] = {}
    by_latin: dict[str, list[dict[str, Any]]] = {}
    for item in data.values():
        if not isinstance(item, dict):
            continue
        name = str(item.get("name", "")).strip()
        if name:
            by_fa.setdefault(normalize_fa(name), []).append(item)
        latin_values = [str(item.get("latin", "")), *map(str, item.get("latinVariants", []))]
        for latin in latin_values:
            key = normalize_latin(latin)
            if key:
                by_latin.setdefault(key, []).append(item)

    total_matches = 0
    successful_languages = 0

    for culture_id, (qid, label) in LANGUAGES.items():
        try:
            bindings = fetch_bindings(qid)
            successful_languages += 1
        except Exception as exc:
            print(f"WARN Wikidata {label} failed: {exc}")
            continue

        culture_matches = 0
        for binding in bindings:
            item_uri = binding.get("item", {}).get("value", "")
            class_uri = binding.get("class", {}).get("value", "")
            fa_label = binding.get("faLabel", {}).get("value", "").strip()
            en_label = binding.get("enLabel", {}).get("value", "").strip()

            candidates: list[dict[str, Any]] = []
            if fa_label and PERSIAN_RE.search(fa_label):
                candidates.extend(by_fa.get(normalize_fa(fa_label), []))
            if not candidates and en_label:
                candidates.extend(by_latin.get(normalize_latin(en_label), []))
            if not candidates:
                continue

            qid_item = entity_id(item_uri)
            class_id = entity_id(class_uri)
            for record in candidates:
                record["usageCultureIds"] = uniq(
                    [*record.get("usageCultureIds", []), culture_id]
                )
                record["classificationSourceIds"] = uniq(
                    [*record.get("classificationSourceIds", []), "wikidata_cc0"]
                )
                record["sourceIds"] = uniq(
                    [*record.get("sourceIds", []), "wikidata_cc0"]
                )
                record["wikidataIds"] = uniq(
                    [*record.get("wikidataIds", []), qid_item]
                )
                if en_label:
                    record["latinVariants"] = uniq(
                        [*record.get("latinVariants", []), en_label]
                    )
                    if not record.get("latin"):
                        record["latin"] = en_label

                wd_gender = CLASS_GENDER.get(class_id, "UNKNOWN")
                if record.get("gender", "UNKNOWN") == "UNKNOWN" and wd_gender != "UNKNOWN":
                    record["gender"] = wd_gender

                record["sourceCount"] = len(uniq(record.get("sourceIds", [])))
                culture_matches += 1
                total_matches += 1

        print(
            f"Wikidata {label}: {len(bindings):,} structured items, "
            f"{culture_matches:,} local record matches"
        )
        time.sleep(0.35)

    DATA_PATH.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        f"Wikidata enrichment complete: {total_matches:,} matches across "
        f"{successful_languages}/{len(LANGUAGES)} reachable language queries"
    )


if __name__ == "__main__":
    main()
