<p align="center"><img src="docs/wordmark.png" width="480" alt="PalManager — Tools to manage your server"></p>

<p align="center">
  <a href="https://github.com/MrDDream/PalManager/releases/latest"><img src="https://img.shields.io/github/v/release/MrDDream/PalManager?color=4CD08A&label=release" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-4CD08A" alt="Android 8.0+">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-6FC3E8" alt="MIT license"></a>
</p>

<p align="center">
  <a href="https://mrddream.github.io/PalManager/en/">Website</a> ·
  <a href="https://github.com/MrDDream/PalManager/releases/latest">Download the APK</a> ·
  <a href="README.fr.md">Lire en français</a>
</p>

**PalManager** is an Android admin app for dedicated **Palworld** servers. It talks to the
official Palworld REST API and to the REST API of the
**[PalDefender](https://github.com/UltimeIT/PalDefender)** mod, so you can run your server from
your phone: watch players, give items and Pals, moderate, broadcast, and — over SFTP — read logs,
manage files and build Pal templates.

> ⚠️ Unofficial fan project, not affiliated with Pocketpair. Game data (item and Pal names and
> images) comes from [paldb.cc](https://paldb.cc) and remains the property of its rightful
> owners — see [License](#-license).

<p align="center"><img src="docs/hero.png" width="760" alt="Give an item, Dashboard and Pal Creator screens"></p>

## ✨ Features

### Server

- 📊 **Dashboard** — address, online players with avatars, FPS and frame time, uptime and in-game
  day, Palworld/PalDefender versions, one-tap shortcuts to every tool, world save.
- 🔌 **Power** — schedule a clean shutdown with a delay and a message to players (your process
  manager restarts the server), or force-stop it with a hold-to-confirm button.
- 🖥️ **My servers** — several server profiles, each with its live status (online/offline, players,
  FPS, latency) and the APIs it has configured (REST, PalDefender, SFTP).
- ✅ **Verify** — one tap on the add/edit server screen checks that the Palworld API, the
  PalDefender API and SFTP are reachable with the credentials you just typed.

### Players

- 👥 **Connected players** — live list with search (name, ID, IP) and sorting (level, ping,
  name), private message, kick and ban.
- 🪪 **Player sheet** — tabs for the team (IVs, Pal Souls, active skills, passives), inventory,
  Palbox, progression and technologies.

### Give

- 🎁 **Items, Pals and NPCs** — pick the recipient once, search a bundled catalog by name or
  picture, filter by category, element, rarity or work suitability, set the quantity
  (×1 / ×10 / ×100) or level, and give in one tap. Pal templates from the Pal Creator can be given
  too.

### World & moderation

- 🛡️ **Guilds** — search by guild or member, members online, Pals of each camp, chest,
  expeditions and lab research, and every base pinned on the map.
- 🗺️ **Live map** — players and guild bases on the Palpagos Islands and World Tree maps, with
  zoom and player selection.
- 🚫 **Bans** — ban and unban players and IP addresses.
- 📣 **Broadcast & maintenance** — announcements, alerts and private messages, PalDefender
  config reload, restart/stop, and a danger zone to delete a base.

### 🔐 SFTP toolkit (optional)

Add SSH/SFTP credentials to a server profile to unlock:

- 📜 **Logs** — PalDefender and UE4SS log viewer with search and colored severity.
- 🗂️ **Files** — browse, upload (a file or a whole folder), download, rename, copy, cut, paste
  and delete on the server, with the host key pinned on first connection.
- 🐾 **Pal Creator** — build a PalDefender Pal Template (species, gender, level, lucky/alpha,
  IVs, Pal Souls, active skills, passives) from a form and save it straight to the server's
  template folder — no JSON to write by hand.

### App

- 🌗 Dark, light or system theme; French or English interface.
- 🐞 Optional debug log of network requests, written to a folder you choose, to diagnose
  connection issues.

## 📱 Screenshots

<table>
<tr>
<td align="center"><b>Dashboard</b><br><img src="docs/screenshots/home.png" width="230" alt="Dashboard"></td>
<td align="center"><b>My servers</b><br><img src="docs/screenshots/servers.png" width="230" alt="My servers"></td>
<td align="center"><b>Connected players</b><br><img src="docs/screenshots/players.png" width="230" alt="Connected players"></td>
</tr>
<tr>
<td align="center"><b>Player sheet</b><br><img src="docs/screenshots/player_team.png" width="230" alt="Player sheet"></td>
<td align="center"><b>Give an item</b><br><img src="docs/screenshots/give_item.png" width="230" alt="Give an item"></td>
<td align="center"><b>Give a Pal</b><br><img src="docs/screenshots/give_pals.png" width="230" alt="Give a Pal"></td>
</tr>
<tr>
<td align="center"><b>Guilds</b><br><img src="docs/screenshots/guilds.png" width="230" alt="Guilds"></td>
<td align="center"><b>Live map</b><br><img src="docs/screenshots/livemap.png" width="230" alt="Live map"></td>
<td align="center"><b>Broadcast & maintenance</b><br><img src="docs/screenshots/alert.png" width="230" alt="Broadcast and maintenance"></td>
</tr>
<tr>
<td align="center"><b>SFTP files</b><br><img src="docs/screenshots/sftp.png" width="230" alt="SFTP file manager"></td>
<td align="center"><b>Logs</b><br><img src="docs/screenshots/logs.png" width="230" alt="Logs"></td>
<td align="center"><b>Pal Creator</b><br><img src="docs/screenshots/pal_creator.png" width="230" alt="Pal Creator"></td>
</tr>
</table>

The power sheet and the light theme are on the [website](https://mrddream.github.io/PalManager/en/).

## 📥 Install

1. Download the APK from the [latest release](https://github.com/MrDDream/PalManager/releases/latest)
   and install it (Android 8.0 or later; allow installs from unknown sources if asked).
2. Add a server: host, Palworld REST API port (8212 by default) and admin password, PalDefender
   API port (17993 by default) and token, and optionally SSH/SFTP credentials.
3. Tap **Verify** to check everything is reachable, then save.

## 🖥️ Server requirements

- A Palworld server with the [official REST API](https://docs.palworldgame.com/) enabled
  (`RESTAPIEnabled=True` in `PalWorldSettings.ini`).
- The **[PalDefender](https://github.com/UltimeIT/PalDefender)** mod with its REST API enabled,
  for what the official API doesn't cover (giving items and Pals, technologies, guilds, IP
  bans…).
- Optional: SSH/SFTP access to the server machine, for the SFTP toolkit.

## 🔨 Build

Requirements: JDK 17 and the Android SDK (API 34).

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

### Visual tests (Paparazzi)

[Paparazzi](https://github.com/cashapp/paparazzi) renders the Compose screens on the JVM, without
an emulator. Each screen exposes a stateless `XxxContent` composable, rendered in dark and light
themes by the tests in `app/src/test/java/com/paladmin/screenshots/`:

```bash
./gradlew :app:recordPaparazziDebug   # record the reference snapshots
./gradlew :app:verifyPaparazziDebug   # compare against them
```

The screenshots in `docs/screenshots/` come from the same tests, rendered at full device
resolution:

```bash
PALMANAGER_DOCS_SHOTS=1 ./gradlew :app:cleanTestDebugUnitTest :app:recordPaparazziDebug
```

### Regenerating the item/Pal dataset

`tools/scrape_paldb.py` scrapes paldb.cc to produce `app/src/main/assets/data/*.json` and the
matching images. The bundled catalog is loaded into a Room database (with full-text search) on
first launch.

## 🗂️ Project layout

| Path | Contents |
| --- | --- |
| `app/src/main/java/com/paladmin/ui/` | Compose screens, one package per screen, plus `theme/` (colors, fonts) and `components/` (shared design components) |
| `app/src/main/java/com/paladmin/data/` | REST clients (Palworld, PalDefender), SFTP client, Room database, preferences, repositories |
| `app/src/main/assets/` | Bundled game catalog (JSON) and images |
| `app/src/test/` | Paparazzi screenshot tests and their reference images |
| `docs/` | GitHub Pages website, README images and screenshots |
| `tools/` | Dataset scraping and enrichment scripts |

## 🛠️ Tech stack

Kotlin, Jetpack Compose (Material 3), MVVM, Hilt, Room, Retrofit/OkHttp with
kotlinx.serialization, Coil, Navigation Compose, DataStore, sshj, Paparazzi.

## 📄 License

The code in this repository is released under the [MIT](LICENSE) license.

The game data bundled in `app/src/main/assets/data/` and `app/src/main/assets/images/` (names,
stats, item, Pal and technology images) is extracted from paldb.cc and remains the property of
its respective rights holders (Pocketpair). It is not covered by the MIT license and is included
for non-commercial use, so the tool can work.
