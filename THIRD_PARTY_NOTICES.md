# Third-party notices

App-NameDic automatically reuses only data sources whose reuse conditions are known. `DATA_SOURCES.md` and `app/src/main/assets/data_sources.json` describe which fields are imported into the name corpus. Historical profiles are a separate curated layer: article text is not bundled wholesale; the app stores short rewritten summaries plus reference links.

## nabidam/persian-names

Project: `https://github.com/nabidam/persian-names`

Use: Iranian/Persian name list and gender labels.

License: MIT. Copyright (c) 2024 nabidam.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, subject to inclusion of the copyright and permission notice. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.

## mehdi-haydari/iranianNames

Project: `https://github.com/mehdi-haydari/iranianNames`

Use: Persian names, gender and auxiliary Latin-script spellings.

License: MIT. Copyright (c) 2016 imwho.

The upstream README warns that some English spellings may need correction. App-NameDic therefore treats them as search/display variants, not etymological evidence.

## armanyazdi/persian-names

Project: `https://github.com/armanyazdi/persian-names`

Use: additional Persian male/female name lists.

License: MIT.

Only the Persian gender-separated lists are automatically merged. The separate English lists are not assumed to be positionally aligned with the Persian lists.

## mohammadhejazirad/persian-names

Project: `https://github.com/mohammadhejazirad/persian-names`

Use: additional Persian/Arabic-script first names classified as male, female or unisex.

License: MIT. Copyright (c) 2022-2026 MohammadReza HejaziRad.

App-NameDic parses only the generated text constants containing names. No upstream JavaScript/TypeScript code is executed, and this source is not treated as evidence for meaning or etymology.

## farbodbj/persian-gender-by-name

Project: `https://github.com/farbodbj/persian-gender-by-name`

Use: larger Persian name/gender/English-transliteration corpus.

License: Apache License 2.0.

Upstream license: `https://github.com/farbodbj/persian-gender-by-name/blob/github-master/LICENSE`

App-NameDic records this source separately in `sourceIds` so attribution and provenance remain traceable.

## jadijadi/persianwords

Project: `https://github.com/jadijadi/persianwords`

Use: additional male/female Persian name lists.

License: CC0 1.0 Universal.

The importer discards non-name lines and merge-conflict markers found in the upstream male-name file.

## Wikidata

Website: `https://www.wikidata.org/`

Use: structured name-language associations, given-name class/gender, labels and Q identifiers; one historical profile may also use a Wikidata entity page as its reference link.

License: structured data in Wikidata's main/property/lexeme namespaces is CC0.

Important modeling rule: Wikidata property P407 ("language of work or name") is stored only as a language/culture association. It is not converted into an etymological `origin`.

## Wiktionary / Kaikki.org / Wiktextract

Wiktionary: `https://en.wiktionary.org/`
Kaikki Persian data: `https://kaikki.org/dictionary/Persian/`
Wiktextract: `https://github.com/tatuylonen/wiktextract`

Use: structured Persian `pos=name` facts such as romanization, IPA, given-name gender categories, Persian-name classification and concise explicit origin labels.

License: Wiktionary text/data is made available under CC BY-SA and GFDL; Kaikki makes extracted data available under the same Wiktionary licenses. App-NameDic does not bulk-copy long dictionary prose. Any Wiktionary-derived material remains attributed to Wiktionary/Kaikki and subject to the applicable ShareAlike/GFDL terms. The Android application source code is maintained separately from this attributed data layer.

Suggested academic citation for the extractor:
Tatu Ylonen, "Wiktextract: Wiktionary as Machine-Readable Structured Data", LREC 2022.

## Maani/Dehkhoda-Lexicon

Dataset: `https://huggingface.co/datasets/Maani/Dehkhoda-Lexicon`

Use: Persian lexical synonyms and antonyms for words that exactly match an existing personal-name spelling.

License: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0), as declared by the dataset card.

Important modeling rule: these relations are stored in `lexicalMeaningFa` and `lexicalAntonymsFa` and are displayed as information about the Persian word homographic with the name. They are not promoted to the direct personal-name `meaning` or `origin`. App-NameDic does not import the dataset's English translation output in this enrichment step.

Attribution: Maani/Dehkhoda-Lexicon, derived from Dehkhoda lexical material. Derived lexical data distributed with App-NameDic remains subject to the applicable CC BY-SA 4.0 requirements.

## Official reference: Iranian Civil Registration / Sahim

Website: `https://sahim.sabteahval.ir/`

The interactive name system is treated as a high-priority official verification reference for Iranian naming information. It is not bulk-scraped by the automatic pipeline unless a public/authorized machine-readable interface and reuse terms are available.

## Historical reference links in App-NameDic 2.0

### Encyclopaedia Iranica

Website: `https://www.iranicaonline.org/`

Use: reference checking for selected historical periods and figures. App-NameDic does not distribute copies of Iranica articles. The bundled Persian profile text is independently summarized/rephrased and the relevant source URL is retained for further reading.

### Wikipedia

Website: `https://www.wikipedia.org/`

Use: reference checking and further-reading links for a subset of historical profiles and timeline records. Long article passages are not bundled. The app stores concise rewritten factual summaries and source links rather than reproducing articles.

Wikipedia article pages may contain text under CC BY-SA and other media with separate licenses. Opening a source link takes the user to the external article; App-NameDic does not bundle those external page assets as part of its historical cards.

## Sources intentionally not bundled

Several repositories/services contain potentially useful names but are not automatically imported because their reuse status or distribution format is unsuitable. This includes unlicensed GitHub datasets, scraped collections without a clear data license, remote pickle-only datasets, commercial name databases, derivative wrappers whose underlying dataset rights are unclear, and datasets whose stated provenance involves leaked personal/banking data.

See `DATA_SOURCES.md` for the current decision log.
