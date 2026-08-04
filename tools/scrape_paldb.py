#!/usr/bin/env python3
"""
Génère app/src/main/assets/data/{items,pals}.json + les images d'icônes à partir de paldb.cc.

Script one-shot, à relancer manuellement quand le jeu ajoute du contenu (pas embarqué dans l'app).
paldb.cc n'a pas d'API publique mais son robots.txt autorise le crawl (`Allow: /`), et chaque page
catégorie/Pals liste déjà, en clair dans le HTML, le "Code" technique (celui utilisé par les
commandes give), le slug d'URL, le nom affiché et l'icône — donc pas besoin de visiter chaque
fiche individuelle : une page par catégorie (FR + EN) suffit.

Usage: python3 tools/scrape_paldb.py
Dépendances : uniquement la stdlib (urllib), pour tourner sans pip install.
"""
from __future__ import annotations

import html
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE_URL = "https://paldb.cc"
USER_AGENT = "Mozilla/5.0 (compatible; PalAdminDatasetBuilder/1.0; +https://paldb.cc/fr/)"
REQUEST_DELAY_SECONDS = 1.0

ROOT = Path(__file__).resolve().parent.parent
ASSETS_DATA = ROOT / "app" / "src" / "main" / "assets" / "data"
ASSETS_IMAGES = ROOT / "app" / "src" / "main" / "assets" / "images"

ITEM_CATEGORIES = [
    "Weapon", "Sphere", "Sphere_Module", "Armor", "Accessory", "Material", "Consumable",
    "Ammo", "Ingredient", "Key_Items", "Glider", "Schematic",
]

ITEM_PATTERN = re.compile(
    r'class="itemname" data-hover="\?s=Items%2F([^"]+)" href="([^"]+)">([^<]+)</a>',
)
ITEM_ICON_PATTERN = re.compile(r'<a href="([^"]+)"><img loading="lazy" src="([^"]+\.webp)"')
ITEM_CARD_SPLIT = re.compile(r'(?=<div class="card itemPopup">)')
ITEM_DESCRIPTION_PATTERN = re.compile(r'<div class="card-body py-2">\s*<div>(.*?)</div>\s*</div>', re.DOTALL)
ITEM_STAT_PATTERN = re.compile(
    r'class="bg-dark bg-gradient p-1">(.*?)</span><span class="border p-1">([^<]+)</span>', re.DOTALL,
)
TAG_PATTERN = re.compile(r'<[^>]+>')


def clean_text(raw: str) -> str:
    return html.unescape(TAG_PATTERN.sub("", raw)).strip()


def parse_item_card(block: str) -> tuple[str, dict[str, str]]:
    """Extrait description + stats (Attaque, Résistance...) d'un bloc <div class="card itemPopup">."""
    description_match = ITEM_DESCRIPTION_PATTERN.search(block)
    description = clean_text(description_match.group(1)) if description_match else ""
    stats = {clean_text(label): clean_text(value) for label, value in ITEM_STAT_PATTERN.findall(block)}
    return description, stats

PAL_PATTERN = re.compile(
    r'data-pal-id="([^"]+)" class="itemname" data-hover="[^"]*" href="([^"]+)">([^<]*)</a>',
)
PAL_ICON_PATTERN = re.compile(
    r'data-pal-id="([^"]+)"[^>]*href="[^"]+"><img loading="lazy" src="([^"]+\.webp)"',
)


def fetch(path: str) -> str:
    url = f"{BASE_URL}{path}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", errors="ignore")


def fetch_with_retry(path: str, attempts: int = 3) -> str:
    last_error: Exception | None = None
    for attempt in range(attempts):
        try:
            return fetch(path)
        except (urllib.error.URLError, TimeoutError, ConnectionError) as error:
            last_error = error
            time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"Échec du fetch pour {path}") from last_error


def download_image(url: str, dest: Path) -> bool:
    if dest.exists():
        return True
    dest.parent.mkdir(parents=True, exist_ok=True)
    # cdn.paldb.cc renvoie 403 sans Referer/UA de navigateur, même si robots.txt autorise le crawl.
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Referer": "https://paldb.cc/",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            dest.write_bytes(response.read())
        return True
    except urllib.error.URLError as error:
        print(f"  [!] image indisponible ({error}) : {url}")
        return False


# Items normalement injoignables en jeu normal (copies d'armes réservées aux PNJ, placeholders
# temporaires, variantes remplacées par une version plus récente). Repéré empiriquement : le nom
# anglais vide seul n'est PAS un signal fiable (Leather/Egg/Honey/Arrow/FlameThrower ont un name_en
# vide juste parce que le scrape de la page EN a raté cet item, alors qu'ils sont bien obtenables).
_UNAVAILABLE_ID_PATTERNS = (
    re.compile(r"_NPC(_|$)"),
    re.compile(r"_Tmp$"),
    re.compile(r"_old$", re.IGNORECASE),
)
_UNAVAILABLE_NAME_EN = {"NPC_WEAPON", "PV_ITEMS", "en_Text"}


def is_unavailable_in_game(item: dict) -> bool:
    if any(pattern.search(item["id"]) for pattern in _UNAVAILABLE_ID_PATTERNS):
        return True
    return item["name_en"] in _UNAVAILABLE_NAME_EN


def scrape_items() -> list[dict]:
    # code -> {slug, category, name_fr, name_en, description, stats, icon_url}
    items: dict[str, dict] = {}

    for category in ITEM_CATEGORIES:
        for lang, name_key in (("fr", "name_fr"), ("en", "name_en")):
            page_html = fetch_with_retry(f"/{lang}/{category}")
            icon_by_slug = dict(ITEM_ICON_PATTERN.findall(page_html))
            for code, slug, name in ITEM_PATTERN.findall(page_html):
                entry = items.setdefault(code, {
                    "id": code,
                    "slug": slug,
                    "category": category,
                    "name_fr": "",
                    "name_en": "",
                    "description": "",
                    "stats": {},
                    "icon_url": icon_by_slug.get(slug, ""),
                })
                entry[name_key] = name.strip()
                if not entry["icon_url"]:
                    entry["icon_url"] = icon_by_slug.get(slug, "")

            if lang == "fr":
                # Description/stats affichées en FR uniquement, une fois par item.
                for block in ITEM_CARD_SPLIT.split(page_html)[1:]:
                    match = ITEM_PATTERN.search(block)
                    if not match:
                        continue
                    code = match.group(1)
                    if code not in items or items[code]["description"]:
                        continue
                    description, stats = parse_item_card(block)
                    items[code]["description"] = description
                    items[code]["stats"] = stats

            print(f"  {category} [{lang}] -> {len(items)} items cumulés")
            time.sleep(REQUEST_DELAY_SECONDS)

    available = [item for item in items.values() if not is_unavailable_in_game(item)]
    excluded_count = len(items) - len(available)
    if excluded_count:
        print(f"  {excluded_count} item(s) exclus (non disponibles en jeu normal : NPC/temp/legacy)")
    return sorted(available, key=lambda item: item["id"])


def scrape_pal_style_page(page_path: str, label: str) -> list[dict]:
    """Pals et Humans partagent exactement le même gabarit de carte (data-pal-id)."""
    entries: dict[str, dict] = {}

    for lang, name_key in (("fr", "name_fr"), ("en", "name_en")):
        page_html = fetch_with_retry(f"/{lang}/{page_path}")
        icon_by_id = dict((pal_id, icon) for pal_id, icon in PAL_ICON_PATTERN.findall(page_html))
        for pal_id, slug, name in PAL_PATTERN.findall(page_html):
            entry = entries.setdefault(pal_id, {
                "id": pal_id,
                "slug": slug,
                "name_fr": "",
                "name_en": "",
                "icon_url": icon_by_id.get(pal_id, ""),
            })
            entry[name_key] = name.strip()
            if not entry["icon_url"]:
                entry["icon_url"] = icon_by_id.get(pal_id, "")
        print(f"  {label} [{lang}] -> {len(entries)} cumulés")
        time.sleep(REQUEST_DELAY_SECONDS)

    return sorted(entries.values(), key=lambda entry: entry["id"])


def scrape_pals() -> list[dict]:
    return scrape_pal_style_page("Pals", "Pals")


# PNJ de test/debug repérés à la main (id == nom affiché ET motif test/tester) — contrairement aux
# items, le tag "Not available" de paldb.cc sur cette page signifie juste "non capturable comme un
# Pal", ce qui est normal pour un humain et ne dit rien sur sa disponibilité via /give.
_TEST_NPC_PATTERN = re.compile(r"(?i)(^test|_test$|tester)")


def is_test_npc(entry: dict) -> bool:
    return bool(_TEST_NPC_PATTERN.search(entry["id"]))


def scrape_humans() -> list[dict]:
    humans = scrape_pal_style_page("Humans", "Humans")
    available = [h for h in humans if not is_test_npc(h)]
    excluded_count = len(humans) - len(available)
    if excluded_count:
        print(f"  {excluded_count} PNJ de test exclu(s)")
    return available


# href du menu "Work Suitability" -> clé interne utilisée par PalWorkSuitabilityDto (même clés que
# pour les Pals, cf. PalLabels.JOB_LABELS côté Kotlin).
_HUMAN_JOB_HREF_MAP = {
    "Kindling": "EmitFlame",
    "Watering": "Watering",
    "Planting": "Seeding",
    "Generating_Electricity": "GenerateElectricity",
    "Handiwork": "Handcraft",
    "Gathering": "Collection",
    "Lumbering": "Deforest",
    "Mining": "Mining",
    "Medicine_Production": "ProductMedicine",
    "Cooling": "Cool",
    "Transporting": "Transport",
    "Farming": "MonsterFarm",
}

_HUMAN_VARIANT_SPLIT = re.compile(r'<div class="card itemPopup" data-tabname="[^"]*">')
_HUMAN_WORK_ROW = re.compile(r'<a href="([A-Za-z_]+)"><img[^>]*/>\s*[^<]*</a></div><div><span[^>]*>Lv</span>(\d+)</div>')
_HUMAN_STAT_ROW = re.compile(r'<div>([A-Za-zÀ-ÿ]+)</div>\s*<div>(\d+)(?:\s*&ndash;\s*\d+)?</div>')
_HUMAN_DROP_ROW = re.compile(
    r'<a class="itemname" data-hover="[^"]*" href="([^"]+)">(?:<img[^>]*/>)?([^<]*)</a>\s*'
    r'<td><small class="itemQuantity">(\d+)(?:&ndash;(\d+))?</small>\s*<td>[^<]*<td>\s*(\d+)%',
    re.DOTALL,
)


def scrape_human_details(humans: list[dict]) -> None:
    """Enrichit chaque PNJ avec stats (niveau 80)/métiers/drops, en scrappant une fois par page
    wiki (slug) plutôt que par entrée : paldb.cc documente toutes les variantes cosmétiques d'un
    même PNJ (ex. Islander -> 92 ids) sur une seule page, chacune dans son propre bloc
    <div class="card itemPopup" data-tabname>. Un slug = un fetch, réutilisé pour toutes ses variantes."""
    by_slug: dict[str, list[dict]] = {}
    for human in humans:
        by_slug.setdefault(human["slug"], []).append(human)

    print(f"  Détails PNJ : {len(by_slug)} pages wiki à scraper...")
    for index, (slug, entries) in enumerate(by_slug.items()):
        try:
            page_html = fetch_with_retry(f"/fr/{slug}")
        except Exception as error:  # noqa: BLE001 - un échec isolé ne doit pas interrompre les 158 autres pages
            print(f"  [!] page indisponible pour {slug} : {error}")
            continue

        starts = [m.start() for m in _HUMAN_VARIANT_SPLIT.finditer(page_html)]
        if not starts:
            continue
        block = page_html[starts[0]:starts[1] if len(starts) > 1 else len(page_html)]

        work_suitabilities = [
            {"job": _HUMAN_JOB_HREF_MAP[href], "level": int(level)}
            for href, level in _HUMAN_WORK_ROW.findall(block)
            if href in _HUMAN_JOB_HREF_MAP
        ]

        stats = None
        level80_index = block.find("Level 80")
        if level80_index != -1:
            segment = block[level80_index:level80_index + 700]
            raw_stats = dict(_HUMAN_STAT_ROW.findall(segment))
            if "PV" in raw_stats and "Attaque" in raw_stats and "Défense" in raw_stats:
                stats = {"hp": int(raw_stats["PV"]), "attack": int(raw_stats["Attaque"]), "defense": int(raw_stats["Défense"])}

        drops = [
            {
                "itemId": item_slug,
                "name": clean_text(name),
                "minQuantity": int(min_qty),
                "maxQuantity": int(max_qty) if max_qty else int(min_qty),
                "probability": int(probability),
            }
            for item_slug, name, min_qty, max_qty, probability in _HUMAN_DROP_ROW.findall(block)
        ]

        for entry in entries:
            entry["work_suitabilities"] = work_suitabilities
            entry["stats"] = stats
            entry["drops"] = drops

        if (index + 1) % 40 == 0:
            print(f"  ...{index + 1}/{len(by_slug)} pages PNJ traitées")
        time.sleep(REQUEST_DELAY_SECONDS)


_TECH_TOKEN = re.compile(
    r'<div class="d-flex justify-content-center align-items-center" style="width:32px;"><div>(\d+)</div></div>'
    r'|background-image: url\(([^)]+)\);" data-hover="\?s=Technology/([^"]+)">\s*'
    r'<div class="hoverTechCost badge">(\d+)</div>\s*'
    r'<div class="hoverTechHeader">([^<]+)</div>\s*'
    r'<div class="hoverTechFooter">([^<]+)</div>',
)


def scrape_technologies() -> list[dict]:
    """paldb.cc/fr/Technologies liste, en un seul passage, chaque technologie (id, palier, coût,
    catégorie Structures/Objets, nom) — même page pour toutes, pas de pagination par tech."""
    page_html = fetch_with_retry("/fr/Technologies")
    start = page_html.find("Technologies /")
    content = page_html[start:] if start != -1 else page_html

    current_level = 0
    entries: dict[str, dict] = {}
    for match in _TECH_TOKEN.finditer(content):
        if match.group(1):
            current_level = int(match.group(1))
            continue
        icon_url, tech_id, cost, category, name = match.group(2), match.group(3), match.group(4), match.group(5), match.group(6)
        entries[tech_id] = {
            "id": tech_id,
            "level": current_level,
            "cost": int(cost),
            "category": category,
            "name_fr": clean_text(name),
            "name_en": "",
            "icon_url": icon_url,
        }

    print(f"  Technologies : {len(entries)} entrées ({current_level} paliers)")
    return sorted(entries.values(), key=lambda e: (e["level"], e["id"]))


def write_dataset(name: str, entries: list[dict], image_subdir: str) -> None:
    ASSETS_DATA.mkdir(parents=True, exist_ok=True)
    output_entries = []
    missing_images = 0
    for index, entry in enumerate(entries):
        icon_url = entry.pop("icon_url", "")
        image_file = f"{entry['id']}.webp" if icon_url else ""
        if icon_url and not download_image(icon_url, ASSETS_IMAGES / image_subdir / image_file):
            image_file = ""
            missing_images += 1
        output_entries.append({**entry, "image": image_file})
        if (index + 1) % 200 == 0:
            print(f"  ...{index + 1}/{len(entries)} images traitées")

    output_path = ASSETS_DATA / f"{name}.json"
    output_path.write_text(json.dumps(output_entries, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Écrit {output_path} ({len(output_entries)} entrées, {missing_images} images manquantes)")


def main() -> None:
    print("Scraping des items depuis paldb.cc...")
    items = scrape_items()
    write_dataset("items", items, "items")

    print("Scraping des Pals depuis paldb.cc...")
    pals = scrape_pals()
    write_dataset("pals", pals, "pals")

    print("Scraping des PNJ (Humains) depuis paldb.cc...")
    humans = scrape_humans()
    scrape_human_details(humans)
    write_dataset("humans", humans, "humans")

    print("Scraping des technologies depuis paldb.cc...")
    technologies = scrape_technologies()
    write_dataset("technologies", technologies, "technologies")


if __name__ == "__main__":
    main()
