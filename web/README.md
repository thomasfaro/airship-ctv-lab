# Samsung Tizen / LG webOS CTV Lab

One shared HTML5 application packaged for both TV platforms. It uses the Airship
Web SDK v2 and registers two Embedded Content IDs on the home screen:
`home_top` above `home_banner`, the latter being the ID shared with the native
labs. Both slots are laid out identically, so a Scene published for either one
is rendered in the same box.

## Slot dimensions

Both slots are `width: 100%` inside a section with `padding: 0 60px`
(`src/styles.css`), so each one is **the viewport width minus 120 px** — 1800
CSS px on a 1920 × 1080 TV, at `devicePixelRatio` 1. That width is the same in
every state, so a Scene is always measured against it.

The height is left to the Scene: the slot has no height of its own and grows
with its content. A rendered slot carries a `min-height: 132px` floor, which
gives a Scene authored in percentages something to resolve against. An empty
slot is 1800 × 0.

## Empty slots

A slot with no eligible content renders nothing at all: no placeholder, no
label, no box, no height. Both sections stay in the document so the SDK always
has a full-width target to render into, and `src/app.js` adds
`has-embedded-content` to a section once it sees content attached — a child that
has height, carries text, or is a media element, which is what tells a rendered
Scene apart from the empty wrapper the SDK leaves behind. Dismissing a Scene
empties the slot and collapses the section again.

## Airship configuration

The mobile App Secret must never be embedded in HTML. In the Airship **Training
app** project, enable/configure the Web channel, then copy the values from
**Settings → Channels → Web → Install SDK** into
`config/airship.local.properties`:

```properties
airship.appKey=...
airship.appSecret=... # mobile apps only; never copied into web/dist
airship.site=eu
airship.webToken=...
airship.webVapidPublicKey=...
```

The build generates `airship-config.js` separately for each platform. It
contains only the Web-safe App Key, token and VAPID public key.

At launch the app:

1. Loads Web SDK v2 from the EU or US CDN.
2. Calls `sdk.create()` to create an opted-out Web channel without prompting
   for push.
3. Adds `ctv_lab` and `tizen` / `webos` tags in the `device` tag group.
4. Identifies `thomasfctv`.
5. Tracks the `home` screen.
6. Registers `#airship-home-top` for `home_top` and `#airship-home-banner` for
   `home_banner`. The pairs come from `embeddedSlots` in
   `scripts/build.mjs`, so adding a slot means adding an entry there plus the
   matching `div` in `src/index.html`.

## Build and browser smoke test

```bash
cd web
npm run build
npm run serve
```

Open `http://localhost:4173`. Localhost is treated as a secure context by
desktop browsers. The top-right Lab button shows the channel ID and SDK
diagnostics.

Hosted test app:
<https://airship-ctv-web-lab.netlify.app/?ntl-drawer-state=hidden>

Create an Embedded Content view style for the **Web** channel with ID
`home_banner` or `home_top`, then publish a Web Scene targeting `ctv_lab`,
`tizen`, `webos`, or named user `thomasfctv`. Until a Scene is eligible the home
screen shows the hero and the catalogue with nothing in between, so the Lab
panel diagnostics are what tell an empty slot apart from a broken one.

## Player URL (Scene CTA target)

The player has an addressable URL, so a Web Scene CTA can use a plain URL action
instead of an app-specific scheme:

```
https://airship-ctv-web-lab.netlify.app/play/<film-id>
https://airship-ctv-web-lab.netlify.app/?play=<film-id>
```

Film ids are the ones the native labs route on: `sintel`, `spring`,
`big-buck-bunny`, `tears-of-steel`, `elephants-dream`, `cosmos-laundromat`,
`caminandes-llamigos`, `caminandes-gran-dillama`. An unknown id is ignored and
the home renders, as on Android and tvOS.

The path form is a Netlify rewrite (`/play/*` → `index.html`, generated as
`_redirects`) and `npm run serve` does the same locally, which is why
`index.html` carries `<base href="/">`. Both forms keep the other query
parameters, so `?platform=tizen&ntl-drawer-state=hidden` stays intact and the
TV wrappers can be pointed at a player URL directly. Opening or closing the
player rewrites the address bar through `history.replaceState`, so the URL
always names the visible screen.

## Monthly releases carousel assets

Run `python3 scripts/generate_monthly_banners.py` to regenerate the 1800×560
assets in `assets/airship/monthly-releases`. The build publishes them under
`/assets/airship/monthly-releases/`; `manifest.json` lists each image and its
matching player CTA URL. The first slide is an animated GIF, followed by four
JPEG slides built from official Blender Open Movie stills.

## Channel state per origin

The Web channel is stored per origin, so `localhost:4173` and the Netlify
deployment are two separate channels. Dismissing an embedded Scene through its
cross finishes the schedule for that channel: the slot stays empty on reload
even though the schedule has no audience and no display limit. Republishing the
Scene brings it back for everyone; otherwise reset the channel by clearing site
data for the origin.

Use **Reset channel** in the Lab panel, or add `?reset=1` to any URL. It empties
every IndexedDB object store and `localStorage` before the SDK loads, then
redirects to the clean URL, so the next load creates a new channel.

It empties the stores instead of deleting the databases because
`indexedDB.deleteDatabase` is blocked while a page or the `push-worker.js`
service worker still holds a connection, and a blocked delete stays queued:
every later open waits on it and `window.UA` never settles, with no error. The
app trips a 10 s watchdog on that state, which also covers a tracker blocker
cancelling the SDK download — `aswpsdkeu.com` is listed in EasyPrivacy, so
desktop Chrome with uBlock Origin needs the origin allowlisted.

## Samsung Tizen

Generated application: `web/dist/tizen`.

Prerequisites: Tizen Studio with the TV extension, a Samsung certificate
profile, and a TV emulator or developer-mode device.

The generated package is a hosted Web app pointing to
`https://airship-ctv-web-lab.netlify.app/?platform=tizen&ntl-drawer-state=hidden`,
keeping the SDK in
the HTTPS context it requires. Import `web/dist/tizen` as an existing Tizen
Web project, then run it as a Tizen Web Application. To package from a
configured CLI:

```bash
tizen build-web -- web/dist/tizen
tizen package -t wgt -s <certificate-profile> \
  -- web/dist/tizen/.buildResult
```

## LG webOS

Generated application: `web/dist/webos`.

Prerequisites: webOS TV CLI and a configured emulator or developer-mode TV.
The generated app points to
`https://airship-ctv-web-lab.netlify.app/?platform=webos&ntl-drawer-state=hidden`.

```bash
ares-package web/dist/webos
ares-install --device <device> com.airship.ctvlab.webos_0.1.0_all.ipk
ares-launch --device <device> com.airship.ctvlab.webos
```

## Compatibility test boundary

Airship officially supports Web SDK v2 on major desktop/mobile browsers, not
Tizen or webOS. Both TV packages therefore load the Netlify HTTPS deployment
instead of a local `file://` page. The app reports `window.isSecureContext`,
channel ID, and initialization errors in its Lab panel.

The app implements spatial navigation for its own controls. Once focus enters
the Airship Embedded Scene, it stops intercepting arrow keys so the test
measures the SDK/browser's real D-pad behavior.
