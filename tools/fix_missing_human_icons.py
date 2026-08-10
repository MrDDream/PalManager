#!/usr/bin/env python3
"""
Récupère les icônes manquantes de app/src/main/assets/data/humans.json (image: "") en allant
chercher chaque page individuelle paldb.cc/en/{slug} — ces PNJ (cibles "Recherché"/Wanted
essentiellement) ont leur icône dans un chemin CDN différent (Pal/Texture/PalIcon/NPC/) de celui
que le scraper principal (tools/scrape_paldb.py) reconnaît pour les humains normaux, d'où le trou.

Usage: python3 tools/fix_missing_human_icons.py
Dépendances : uniquement la stdlib (urllib), pour tourner sans pip install.
"""
from __future__ import annotations

import json
import re
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA_FILE = ROOT / "app" / "src" / "main" / "assets" / "data" / "humans.json"
IMAGES_DIR = ROOT / "app" / "src" / "main" / "assets" / "images" / "humans"

# Spécifiquement le dossier NPC : le dossier PalIcon/Normal/ contient aussi une icône générique
# "marchand" (T_PalDealer_icon_normal.webp) qui apparaît plus tôt dans la page (bandeau/nav) et
# matchait à tort en premier avec un motif PalIcon/ trop large.
ICON_PATTERN = re.compile(r"https://cdn\.paldb\.cc/image/Pal/Texture/PalIcon/NPC/[A-Za-z0-9_]+\.webp")
HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; PalAdminDatasetBuilder/1.0)"}


def fetch(url: str) -> bytes:
    request = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def find_icon_url(slug: str) -> str | None:
    html = fetch(f"https://paldb.cc/en/{slug}").decode("utf-8", errors="ignore")
    match = ICON_PATTERN.search(html)
    return match.group(0) if match else None


def main() -> None:
    entries = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    missing = [e for e in entries if not e.get("image", "").strip()]
    print(f"{len(missing)} entrée(s) sans icône sur {len(entries)}")

    fixed = 0
    for i, entry in enumerate(missing):
        slug = entry["slug"]
        try:
            icon_url = find_icon_url(slug)
            if not icon_url:
                print(f"  [!] {entry['id']} ({slug}) : pas d'icône trouvée sur la page")
                continue
            image_bytes = fetch(icon_url)
            dest = IMAGES_DIR / f"{entry['id']}.webp"
            dest.write_bytes(image_bytes)
            entry["image"] = f"{entry['id']}.webp"
            fixed += 1
            print(f"  [{i + 1}/{len(missing)}] {entry['id']} <- {icon_url}")
        except Exception as error:  # noqa: BLE001 - script one-shot, on log et on continue
            print(f"  [!] {entry['id']} ({slug}) : échec ({error})")
        time.sleep(0.2)

    DATA_FILE.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Terminé : {fixed}/{len(missing)} icônes récupérées. {DATA_FILE} mis à jour.")


if __name__ == "__main__":
    main()
