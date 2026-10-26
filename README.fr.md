<p align="center"><img src="docs/wordmark.png" width="480" alt="PalManager — Tools to manage your server"></p>

*[Read in English](README.md)*

Application Android d'administration pour serveurs dédiés **Palworld**, pilotée via l'API REST
officielle Palworld et l'API REST du mod **[PalDefender](https://github.com/UltimeIT/PalDefender)**.

> ⚠️ Projet non-officiel, sans lien avec Pocketpair. Les données de jeu (noms/images d'objets et de
> Pals) proviennent de [paldb.cc](https://paldb.cc) et restent la propriété de leurs ayants droit —
> voir [Licence](#licence).

<p align="center"><img src="docs/hero.png" width="760" alt="Écrans Donner un item, Dashboard et Créateur de Pal"></p>

## ✨ Fonctionnalités

- 📊 **Dashboard** : adresse du serveur, joueurs en ligne (avec avatars), FPS, durée en ligne,
  versions Palworld/PalDefender, raccourcis en un geste, sauvegarde, et une feuille Alimentation
  pour programmer un redémarrage ou forcer l'arrêt (appui maintenu).
- 🖥️ **Mes serveurs** : chaque profil affiche son état en direct (en ligne/hors ligne, joueurs,
  FPS, latence) et les API configurées (REST, PalDefender, SFTP).
- 👥 **Joueurs connectés** : liste en temps réel avec recherche et tri (niveau, latence, nom),
  kick/ban, message privé, et une fiche joueur à onglets (équipe, inventaire, Palbox, progression,
  technologies). Les Pals de l'équipe se déplient pour montrer IV, Âme de Pal, compétences actives
  et passifs.
- 🛡️ **Guildes** : liste des membres (recherche par nom de guilde ou de membre), toutes les bases
  épinglées sur la carte en direct, Pals de chaque camp avec fiche détaillée, coffre, expéditions
  et progression des recherches de labo.
- 🎁 **Donner un item / un Pal / un PNJ** : choix du destinataire une seule fois, recherche par nom
  ou image dans un catalogue embarqué, filtres par catégorie/élément/rareté/métier, quantité
  (×1/×10/×100) ou niveau, et don en un geste.
- 🗺️ **Carte en direct** : position des joueurs sur la carte des Îles Palpagos.
- 🚫 **Bannissements** (joueurs + IP).
- 📣 **Diffusion & maintenance** : broadcast, alertes, messages, zone danger (reload config,
  suppression de base).
- 🐞 **Mode debug** : log réseau optionnel écrit dans un dossier de votre choix, pour diagnostiquer
  les problèmes de connexion.
- 🌗 Thème clair/sombre, interface FR/EN (appliquée aussi aux données de jeu).
- 🎨 **Nouvelle interface** : refonte complète — palette sombre, typographies
  Sora/Inter/JetBrains Mono, cibles tactiles de 44 dp, feuilles du bas plutôt que des dialogues
  empilés.

### 🔐 Boîte à outils SFTP (optionnelle)

Renseignez des identifiants SSH/SFTP sur un profil serveur pour débloquer :

- 📜 **Logs** : visionneuse de logs PalDefender/UE4SS avec recherche et sévérité en couleur.
- 🗂️ **SFTP** : parcourir/upload (fichier seul ou dossier entier)/télécharger/renommer/copier/
  couper/coller/supprimer sur le système de fichiers du serveur, avec épinglage de la clé hôte
  à la première connexion (TOFU).
- 🐾 **Créateur de Pal** : construire un fichier Pal Template PalDefender (espèce, sexe, niveau,
  chanceux/alpha, IV, Âme de Pal, compétences actives, passifs) depuis un formulaire et
  l'enregistrer directement dans le dossier templates du serveur — plus de JSON à écrire à la main.
- ✅ **Vérifier** : un bouton sur l'écran d'ajout/modification de serveur qui teste en un clic que
  l'API Palworld, l'API PalDefender et le SFTP (si configuré) sont bien joignables avec les
  identifiants tout juste saisis.

## 📱 Aperçu

<table>
<tr>
<td align="center"><b>Dashboard</b><br><img src="docs/screenshots/home.png" width="230"></td>
<td align="center"><b>Mes serveurs</b><br><img src="docs/screenshots/servers.png" width="230"></td>
<td align="center"><b>Joueurs connectés</b><br><img src="docs/screenshots/players.png" width="230"></td>
</tr>
<tr>
<td align="center"><b>Fiche joueur</b><br><img src="docs/screenshots/player_team.png" width="230"></td>
<td align="center"><b>Donner un item</b><br><img src="docs/screenshots/give_item.png" width="230"></td>
<td align="center"><b>Donner un Pal</b><br><img src="docs/screenshots/give_pals.png" width="230"></td>
</tr>
<tr>
<td align="center"><b>Guildes</b><br><img src="docs/screenshots/guilds.png" width="230"></td>
<td align="center"><b>Carte en direct</b><br><img src="docs/screenshots/livemap.png" width="230"></td>
<td align="center"><b>Diffusion & maintenance</b><br><img src="docs/screenshots/alert.png" width="230"></td>
</tr>
<tr>
<td align="center"><b>Gestionnaire SFTP</b><br><img src="docs/screenshots/sftp.png" width="230"></td>
<td align="center"><b>Logs</b><br><img src="docs/screenshots/logs.png" width="230"></td>
<td align="center"><b>Créateur de Pal</b><br><img src="docs/screenshots/pal_creator.png" width="230"></td>
</tr>
</table>

## 🛠️ Stack technique

Kotlin + Jetpack Compose (Material 3), MVVM, Hilt, Room, Retrofit/OkHttp + kotlinx.serialization,
Coil, Jetpack Navigation Compose, DataStore Preferences.

## 🖥️ Prérequis serveur

- Un serveur Palworld avec l'[API REST officielle](https://docs.palworldgame.com/) activée
  (`RESTAPIEnabled=True` dans `PalWorldSettings.ini`).
- Le mod **[PalDefender](https://github.com/UltimeIT/PalDefender)** installé et son API REST
  activée, pour les fonctionnalités que l'API officielle ne couvre pas (give item/Pal, techs,
  guildes, bans IP...).

## 🔨 Build

```bash
./gradlew assembleDebug
```

L'APK debug est généré dans `app/build/outputs/apk/debug/`.

### Tests visuels (Paparazzi)

Le projet embarque [Paparazzi](https://github.com/cashapp/paparazzi) pour rendre les composables
Compose sur JVM sans émulateur (utile en dev pour vérifier rapidement un écran) :

```bash
./gradlew :app:recordPaparazziDebug
```

Chaque écran expose un composable sans état `XxxContent`, rendu par les tests de
`app/src/test/java/com/paladmin/screenshots/` en thème sombre et clair. Les captures du README et
du site (`docs/screenshots/`) viennent des mêmes tests, rendus à la résolution réelle de l'appareil :

```bash
PALMANAGER_DOCS_SHOTS=1 ./gradlew :app:cleanTestDebugUnitTest :app:recordPaparazziDebug
```

### Régénérer le dataset items/Pals

`tools/scrape_paldb.py` scrape paldb.cc/fr pour produire `app/src/main/assets/data/*.json` +
les images associées (catalogue embarqué, chargé une fois au premier lancement dans une base
Room avec recherche FTS).

## 📄 Licence

Le code de ce dépôt est publié sous licence [MIT](LICENSE).

Les données de jeu embarquées dans `app/src/main/assets/data/` et `app/src/main/assets/images/`
(noms, statistiques, images d'objets/Pals/technologies) sont extraites de paldb.cc et restent la
propriété de leurs ayants droit respectifs (Pocketpair). Elles ne sont pas couvertes par la
licence MIT et sont incluses ici à titre d'usage non commercial, pour le fonctionnement de l'outil.
