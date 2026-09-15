# CTV Lab

Apps de test type streaming pour le SDK Airship sur **Google TV**, **tvOS**,
**Samsung Tizen** et **LG webOS**. Catalogue, player (stream de démo), écran
Lab avec channel ID / named user.

## 1. Credentials Airship

```bash
cp config/airship.local.properties.example config/airship.local.properties
```

Remplis `airship.appKey`, `airship.appSecret`, et `airship.site` (`eu` ou `us`) depuis le dashboard Airship → Project details.

Sans ça, le catalogue et le player marchent quand même ; le SDK ne s’initialise pas.

Le lab HTML5 utilise le même projet mais nécessite aussi les credentials du
canal Web (`airship.webToken` et `airship.webVapidPublicKey`). L’App Secret
mobile n’est jamais intégré au JavaScript.

## 2. Google TV — Android Studio

Ouvre le dossier `android/` dans Android Studio.

### Simulateur Google TV (recommandé)

Device Manager → Create Device → catégorie **TV** → **Google TV** (ou Android TV) → image **Google APIs / Google TV**, ABI **arm64-v8a** ou **x86_64**.

Si l’image n’est pas installée :

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "system-images;android-36;google-tv;arm64-v8a"
```

Puis Run sur cet AVD. Dans le launcher TV, l’app s’appelle **CTV Lab**.

En CLI (Java 21 recommandé) :

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
./gradlew :app:installDebug
```

Logs SDK : filtre Logcat sur `CtvLab` ou `UALib`.

## 3. tvOS — Xcode

Le runtime **tvOS n’est pas installé** par défaut (Xcode 26 n’a ici que iOS). Une fois :

**Xcode → Settings → Components → tvOS** (ou `xcodebuild -downloadPlatform tvOS`)

Génère et ouvre le projet :

```bash
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
cd apple
xcodegen generate
open CTVLab.xcodeproj
```

Dans Signing & Capabilities, choisis ton Team (Automatic).

## 4. Quoi tester

1. Lance l’app → écran catalogue.
2. Ouvre **Outils → Airship Lab** → copie le **channel ID**.
3. Dans Airship, crée une **Scene** (pas de HTML sur tvOS, pas d’URL externe sur Google TV) ciblant le tag `ctv_lab`.
4. Relance l’app / ouvre un titre : la Scene doit s’afficher par-dessus le catalogue ou le player.
5. Optionnel : named user dans le Lab, puis `lookup_channel` via le MCP.

Tags posés automatiquement : `ctv_lab` + `google_tv` ou `tvos`.

Google TV n’a **pas** de push visible. tvOS : badge / silent push seulement.

## 5. Samsung Tizen / LG webOS — Web SDK v2 expérimental

Le SDK Web ne supporte pas officiellement ces navigateurs TV. Le dossier
`web/` contient une même app HTML5 packagée pour les deux OS afin d’évaluer
des Scenes Embedded Content (`home_top` puis `home_banner`), leur rendu et leur
navigation D-pad.

```bash
cd web
npm run build
npm run serve
```

Voir `web/README.md` pour les credentials du canal Web et les commandes de
packaging Tizen / webOS.

## Structure

```
android/     Compose for TV + ExoPlayer + Airship SDK 20
apple/       SwiftUI tvOS + AVPlayer + Airship SDK 20
web/         Web SDK v2 + packages Samsung Tizen / LG webOS
config/      credentials partagées
```
