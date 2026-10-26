<p align="center"><img src="docs/wordmark.png" width="480" alt="PalManager — Tools to manage your server"></p>

*[Lire en français](README.fr.md)*

Android admin app for dedicated **Palworld** servers, driven through the official Palworld
REST API and the **[PalDefender](https://github.com/UltimeIT/PalDefender)** mod's REST API.

> ⚠️ Unofficial fan project, not affiliated with Pocketpair. Game data (item/Pal names and images)
> comes from [paldb.cc](https://paldb.cc) and remains the property of its rightful owners — see
> [License](#license).

<p align="center"><img src="docs/hero.png" width="760" alt="Give an item, Dashboard and Pal Creator screens"></p>

## ✨ Features

- 📊 **Dashboard**: server address, online players (with avatars), FPS, uptime,
  Palworld/PalDefender versions, one-tap shortcuts, save, and a Power sheet to schedule a restart
  or force-stop (hold-to-confirm).
- 🖥️ **My servers**: every profile shows its live status (online/offline, players, FPS, latency)
  and which APIs are configured (REST, PalDefender, SFTP).
- 👥 **Connected players**: live list with search and sort (level, ping, name), kick/ban, private
  message, and a tabbed player sheet (team, inventory, Palbox, progression, technologies). Team
  Pals expand to show IVs, Pal Souls, active skills and passives.
- 🛡️ **Guilds**: member list (searchable by guild or member name), all base locations pinned on the
  live map, per-camp Pal roster with detail view, chest, expeditions and lab research progress.
- 🎁 **Give item / Pal / NPC**: pick the recipient once, then search a bundled catalog by name or
  image with filters by category/element/rarity/job, set the quantity (×1/×10/×100) or level, and
  give in one tap.
- 🗺️ **Live map**: player positions on the Palpagos Islands map.
- 🚫 **Bans** (players + IP).
- 📣 **Broadcast & maintenance**: broadcast, alerts, messages, danger zone (reload config,
  delete base).
- 🐞 **Debug logging**: optional network request/response log written to a folder you choose, to
  help diagnose connectivity issues.
- 🌗 Light/dark theme, FR/EN interface (consistently applied to in-game data too).
- 🎨 **New UI**: full redesign — dark-first palette, Sora/Inter/JetBrains Mono typography,
  44 dp touch targets, bottom sheets instead of stacked dialogs.

### 🔐 SFTP toolkit (optional)

Configure SSH/SFTP credentials on a server profile to unlock:

- 📜 **Logs**: PalDefender/UE4SS log viewer with search and colored severity.
- 🗂️ **SFTP**: browse/upload (single file or whole folder)/download/rename/copy/cut/paste/delete on
  the server's filesystem, with trust-on-first-use host key pinning.
- 🐾 **Pal Creator**: build a PalDefender Pal Template file (species, gender, level, shiny/alpha,
  IVs, Pal Souls, active skills, passives) from a form and save it straight to the server's
  template folder — no manual JSON editing.
- ✅ **Verify**: a one-tap check on the Add/Edit server screen that the Palworld API, PalDefender
  API, and SFTP (if configured) are all reachable with the credentials you just typed in.

## 📱 Screenshots

<table>
<tr>
<td align="center"><b>Dashboard</b><br><img src="docs/screenshots/home.png" width="230"></td>
<td align="center"><b>My servers</b><br><img src="docs/screenshots/servers.png" width="230"></td>
<td align="center"><b>Connected players</b><br><img src="docs/screenshots/players.png" width="230"></td>
</tr>
<tr>
<td align="center"><b>Player sheet</b><br><img src="docs/screenshots/player_team.png" width="230"></td>
<td align="center"><b>Give an item</b><br><img src="docs/screenshots/give_item.png" width="230"></td>
<td align="center"><b>Give a Pal</b><br><img src="docs/screenshots/give_pals.png" width="230"></td>
</tr>
<tr>
<td align="center"><b>Guilds</b><br><img src="docs/screenshots/guilds.png" width="230"></td>
<td align="center"><b>Live map</b><br><img src="docs/screenshots/livemap.png" width="230"></td>
<td align="center"><b>Broadcast & maintenance</b><br><img src="docs/screenshots/alert.png" width="230"></td>
</tr>
<tr>
<td align="center"><b>SFTP file manager</b><br><img src="docs/screenshots/sftp.png" width="230"></td>
<td align="center"><b>Logs</b><br><img src="docs/screenshots/logs.png" width="230"></td>
<td align="center"><b>Pal Creator</b><br><img src="docs/screenshots/pal_creator.png" width="230"></td>
</tr>
</table>

## 🛠️ Tech stack

Kotlin + Jetpack Compose (Material 3), MVVM, Hilt, Room, Retrofit/OkHttp + kotlinx.serialization,
Coil, Jetpack Navigation Compose, DataStore Preferences.

## 🖥️ Server requirements

- A Palworld server with the [official REST API](https://docs.palworldgame.com/) enabled
  (`RESTAPIEnabled=True` in `PalWorldSettings.ini`).
- The **[PalDefender](https://github.com/UltimeIT/PalDefender)** mod installed with its REST API
  enabled, for the features the official API doesn't cover (give item/Pal, techs, guilds,
  IP bans...).

## 🔨 Build

```bash
./gradlew assembleDebug
```

The debug APK is generated in `app/build/outputs/apk/debug/`.

### Visual tests (Paparazzi)

The project bundles [Paparazzi](https://github.com/cashapp/paparazzi) to render Compose
composables on the JVM without an emulator (handy in dev to quickly check a screen):

```bash
./gradlew :app:recordPaparazziDebug
```

Each screen exposes a stateless `XxxContent` composable rendered by the tests in
`app/src/test/java/com/paladmin/screenshots/`, in dark and light themes. The README/website
screenshots in `docs/screenshots/` come from the same tests rendered at full device resolution:

```bash
PALMANAGER_DOCS_SHOTS=1 ./gradlew :app:cleanTestDebugUnitTest :app:recordPaparazziDebug
```

### Regenerating the item/Pal dataset

`tools/scrape_paldb.py` scrapes paldb.cc/fr to produce `app/src/main/assets/data/*.json` +
the associated images (bundled catalog, loaded once on first launch into a Room database with
FTS search).

## 📄 License

This repo's code is published under the [MIT](LICENSE) license.

The game data bundled in `app/src/main/assets/data/` and `app/src/main/assets/images/`
(names, stats, item/Pal/technology images) is extracted from paldb.cc and remains the property
of its respective rights holders (Pocketpair). It is not covered by the MIT license and is
included here for non-commercial use, to make the tool work.
