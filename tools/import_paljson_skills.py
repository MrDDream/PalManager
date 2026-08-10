#!/usr/bin/env python3
"""
Génère app/src/main/assets/data/{active_skills,passives}.json depuis le projet PalJSON du même
auteur (github.com/MrDDream/PalJSON) : source bien plus fiable que paldb.cc pour ces deux catalogues
(couvre les passifs négatifs "_downN" et les variantes "ElementBoost_*_PAL", absents de paldb.cc,
et donne id + nom FR/EN directement, sans indirection par le Fruit/l'implant qui l'enseigne).

Script one-shot, à relancer manuellement si PalJSON met à jour ses données (pas embarqué dans l'app).

Usage: python3 tools/import_paljson_skills.py
Dépendances : uniquement la stdlib (urllib), pour tourner sans pip install.
"""
from __future__ import annotations

import json
import re
import urllib.request
from pathlib import Path

# PalJSON reprend parfois telles quelles les balises de coloration du jeu (ex.
# "<NumBlue_13>+</>50.0 %", "<Status_Up>Immunité</>") dans les descriptions FR de passifs —
# du texte riche interne à Palworld, jamais résolu côté PalJSON. On les retire, seul le contenu
# visible nous intéresse ici (pas de rendu riche dans l'app).
_GAME_TAG = re.compile(r"<[^>]*>")


def strip_game_tags(text: str) -> str:
    return re.sub(r"\s+", " ", _GAME_TAG.sub("", text)).strip()

RAW_BASE = "https://raw.githubusercontent.com/MrDDream/PalJSON/main/data"
ROOT = Path(__file__).resolve().parent.parent
ASSETS_DATA = ROOT / "app" / "src" / "main" / "assets" / "data"


def fetch_js_array(filename: str, var_name: str) -> list[dict]:
    """Les fichiers data/*.js de PalJSON sont "window.VAR = [...];" — le contenu du tableau est du
    JSON valide (clés/chaînes entre guillemets doubles), on le parse tel quel après avoir coupé
    l'assignation JS autour."""
    url = f"{RAW_BASE}/{filename}"
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (compatible; PalAdminDatasetBuilder/1.0)"})
    with urllib.request.urlopen(request, timeout=30) as response:
        text = response.read().decode("utf-8")
    prefix = f"window.{var_name} = "
    if not text.startswith(prefix):
        raise ValueError(f"{filename}: préfixe inattendu, format PalJSON a peut-être changé")
    return json.loads(text[len(prefix):].strip().rstrip(";"))


def main() -> None:
    print("Import des compétences actives depuis PalJSON...")
    skills = fetch_js_array("skills.js", "PJ_SKILLS")
    active_skills = sorted(
        (
            {
                "id": s["a"],
                "name_fr": s.get("fn") or s["n"],
                "name_en": s["n"],
                "element": s.get("el", ""),
                "power": s.get("pw", 0),
                "cooldown": s.get("cd", 0),
            }
            for s in skills
        ),
        key=lambda e: e["id"],
    )
    (ASSETS_DATA / "active_skills.json").write_text(
        json.dumps(active_skills, ensure_ascii=False, indent=2), encoding="utf-8",
    )
    print(f"  Écrit {ASSETS_DATA / 'active_skills.json'} ({len(active_skills)} compétences)")

    print("Import des compétences passives depuis PalJSON...")
    passives = fetch_js_array("passives.js", "PJ_PASSIVES")

    def build_passive(p: dict) -> dict:
        description = strip_game_tags(p.get("d", ""))
        # "fd" (description FR) absent sur une partie des entrées PalJSON -> repli EN. Repli EN
        # aussi si "fd" contient une variable non résolue (ex. "+{EffectValue4} %") — la version EN
        # est fiable, PalJSON ne l'a simplement jamais traduite pour ce passif.
        description_fr = strip_game_tags(p.get("fd") or p.get("d", ""))
        if "{" in description_fr:
            description_fr = description
        return {
            "id": p["a"],
            "name_fr": p.get("fn") or p["n"],
            "name_en": p["n"],
            "rank": p.get("r", 0),
            "description": description,
            "description_fr": description_fr,
        }

    passive_entries = sorted((build_passive(p) for p in passives), key=lambda e: e["id"])
    (ASSETS_DATA / "passives.json").write_text(
        json.dumps(passive_entries, ensure_ascii=False, indent=2), encoding="utf-8",
    )
    print(f"  Écrit {ASSETS_DATA / 'passives.json'} ({len(passive_entries)} passifs)")


if __name__ == "__main__":
    main()
