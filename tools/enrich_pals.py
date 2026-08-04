#!/usr/bin/env python3
"""
Enrichit app/src/main/assets/data/pals.json avec élément/rareté/stats/métiers/compétence de
partenaire/localisation — repris tels quels des fichiers de données déjà extraits du jeu par le
projet PalSite (src/gamedata/{pals,pal-details}.json, eux-mêmes sourcés de paldeck.cc et des
pages paldb.cc/fr par PalSite), plutôt que de re-scraper 300 fiches individuelles nous-mêmes.

Usage: python3 tools/enrich_pals.py /chemin/vers/PalSite
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PALS_JSON = ROOT / "app" / "src" / "main" / "assets" / "data" / "pals.json"


def main() -> None:
    if len(sys.argv) != 2:
        print("Usage: python3 tools/enrich_pals.py /chemin/vers/PalSite")
        raise SystemExit(1)

    palsite_root = Path(sys.argv[1])
    catalog = json.loads((palsite_root / "src/gamedata/pals.json").read_text())["pals"]
    details = json.loads((palsite_root / "src/gamedata/pal-details.json").read_text())

    catalog_by_id = {entry["id"]: entry for entry in catalog}

    pals = json.loads(PALS_JSON.read_text())
    enriched_count = 0
    for pal in pals:
        catalog_entry = catalog_by_id.get(pal["id"])
        detail_entry = details.get(pal["id"])
        if catalog_entry is None and detail_entry is None:
            continue
        enriched_count += 1
        pal["element1"] = catalog_entry["element1"] if catalog_entry else "Normal"
        pal["element2"] = (catalog_entry.get("element2") if catalog_entry else None) or None
        pal["rarity"] = catalog_entry["rarity"] if catalog_entry else 1
        pal["zukanIndex"] = catalog_entry["zukanIndex"] if catalog_entry else -1
        if detail_entry:
            pal["stats"] = detail_entry["stats"]
            pal["workSuitabilities"] = detail_entry["workSuitabilities"]
            pal["partnerSkill"] = detail_entry["partnerSkill"]
            pal["locations"] = detail_entry["locations"]
            pal["mapPosition"] = detail_entry["mapPosition"]
        else:
            pal["stats"] = None
            pal["workSuitabilities"] = []
            pal["partnerSkill"] = None
            pal["locations"] = []
            pal["mapPosition"] = None

    PALS_JSON.write_text(json.dumps(pals, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Enrichi {enriched_count}/{len(pals)} pals ({PALS_JSON})")


if __name__ == "__main__":
    main()
