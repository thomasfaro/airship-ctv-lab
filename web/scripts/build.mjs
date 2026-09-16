import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(here, "..");
const repoRoot = resolve(webRoot, "..");
const source = join(webRoot, "src");
const output = join(webRoot, "dist");

const LIVE_SITE_URL = "https://airship-ctv-web-lab.netlify.app";
const EMBEDDED_SLOTS = [
  { selector: "#airship-home-top", embeddedId: "home_top" },
  { selector: "#airship-home-banner", embeddedId: "home_banner" },
];
const WEB_ROUTES = [
  `${LIVE_SITE_URL}/`,
  `${LIVE_SITE_URL}/play/<film-id>`,
  `${LIVE_SITE_URL}/?play=<film-id>`,
  `${LIVE_SITE_URL}/?platform=tizen&ntl-drawer-state=hidden`,
  `${LIVE_SITE_URL}/?platform=webos&ntl-drawer-state=hidden`,
  `${LIVE_SITE_URL}/?reset=1`,
];

const PLACEHOLDER_VALUES = new Set([
  "",
  "YOUR_APP_KEY",
  "YOUR_APP_SECRET",
  "YOUR_WEB_TOKEN",
  "YOUR_WEB_VAPID_PUBLIC_KEY",
]);

// Web-channel values already served by the live lab. This is a demo app: they
// are baked in so Netlify can build without env vars or the gitignored
// properties file. The mobile App Secret is not among them and must stay out.
const DEMO_WEB_CREDENTIALS = {
  "airship.appKey": "YHUkQYpDRbCnHeXXYvQcKg",
  "airship.webToken":
    "MTpZSFVrUVlwRFJiQ25IZVhYWXZRY0tnOnRZMXc3eFVzZVBjcVFhZ0hwZFFGNHc4SmJOTFdCWUVHQ2l5ZGRMQkItbms",
  "airship.webVapidPublicKey":
    "BC3RGg_3FrplDsju5H8IivlIiHHQgRqoofCoDa2qGNygslCRv7jya5T6PTLX4oEeCeIoT_sUdLXUZaG2OJ94YPE=",
  "airship.site": "eu",
};

function fileValue(properties, key) {
  const value = properties[key];
  if (!value || PLACEHOLDER_VALUES.has(value)) return "";
  return value;
}

async function readProperties(path) {
  try {
    const text = await readFile(path, "utf8");
    return Object.fromEntries(
      text
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line && !line.startsWith("#"))
        .map((line) => {
          const separator = line.indexOf("=");
          return separator === -1
            ? [line, ""]
            : [line.slice(0, separator).trim(), line.slice(separator + 1).trim()];
        }),
    );
  } catch (error) {
    if (error.code === "ENOENT") return {};
    throw error;
  }
}

// Local builds may still overlay optional Web extras from
// config/airship.local.properties. airship.appSecret is dropped even if present.
function webCredentials(fileProperties) {
  const properties = { ...fileProperties };
  delete properties["airship.appSecret"];

  return {
    "airship.appKey":
      fileValue(properties, "airship.appKey") || DEMO_WEB_CREDENTIALS["airship.appKey"],
    "airship.site":
      fileValue(properties, "airship.site") || DEMO_WEB_CREDENTIALS["airship.site"],
    "airship.webToken":
      fileValue(properties, "airship.webToken") || DEMO_WEB_CREDENTIALS["airship.webToken"],
    "airship.webVapidPublicKey":
      fileValue(properties, "airship.webVapidPublicKey") ||
      DEMO_WEB_CREDENTIALS["airship.webVapidPublicKey"],
    "airship.webDefaultIcon": fileValue(properties, "airship.webDefaultIcon"),
    "airship.webDefaultTitle": fileValue(properties, "airship.webDefaultTitle"),
    "airship.webDefaultActionURL": fileValue(properties, "airship.webDefaultActionURL"),
  };
}

function runtimeConfig(properties, platform) {
  return `window.CTVLAB_CONFIG = ${JSON.stringify(
    {
      appKey: properties["airship.appKey"] ?? "",
      token: properties["airship.webToken"] ?? "",
      vapidPublicKey: properties["airship.webVapidPublicKey"] ?? "",
      site: properties["airship.site"] ?? "eu",
      platform,
      liveSiteUrl: LIVE_SITE_URL,
      embeddedSlots: EMBEDDED_SLOTS,
      customComponents: [
        "Airship Web SDK embeddedViews",
        "App-owned Lab panel, HTML5 player and spatial D-pad navigation",
      ],
      availableUrls: WEB_ROUTES,
      deepLinks: [
        `${LIVE_SITE_URL}/play/<film-id>`,
        `${LIVE_SITE_URL}/?play=<film-id>`,
        "ctvlab://… is native-only (Google TV / tvOS)",
      ],
      namedUser: "thomasfctv",
    },
    null,
    2,
  )};\n`;
}

function pushWorker(properties) {
  const site = (properties["airship.site"] ?? "eu").toLowerCase();
  const sdkUrl =
    site === "eu"
      ? "https://aswpsdkeu.com/notify/v2/ua-sdk.min.js"
      : "https://aswpsdkus.com/notify/v2/ua-sdk.min.js";
  const workerConfig = {
    defaultIcon: properties["airship.webDefaultIcon"] ?? "",
    defaultTitle: properties["airship.webDefaultTitle"] ?? "CTV Lab",
    defaultActionURL: properties["airship.webDefaultActionURL"] ?? "",
    appKey: properties["airship.appKey"] ?? "",
    token: properties["airship.webToken"] ?? "",
    vapidPublicKey: properties["airship.webVapidPublicKey"] ?? "",
  };

  return `importScripts(${JSON.stringify(sdkUrl)});\n` +
    `uaSetup.worker(self, ${JSON.stringify(workerConfig, null, 2)});\n`;
}

async function copyApp(target, properties, platform) {
  await mkdir(target, { recursive: true });
  await cp(source, target, { recursive: true });
  await writeFile(join(target, "airship-config.js"), runtimeConfig(properties, platform));
  await writeFile(join(target, "push-worker.js"), pushWorker(properties));
}

const properties = webCredentials(
  await readProperties(join(repoRoot, "config", "airship.local.properties")),
);

await rm(output, { recursive: true, force: true });

const browser = join(output, "browser");
const tizen = join(output, "tizen");
const webos = join(output, "webos");

await copyApp(browser, properties, "browser");
// Netlify serves /play/<film-id> as the app so a Scene CTA can link to a real URL.
await writeFile(join(browser, "_redirects"), "/play/*  /index.html  200\n");
await cp(join(webRoot, "assets"), join(browser, "assets"), { recursive: true });
await copyApp(tizen, properties, "tizen");
await copyApp(webos, properties, "webos");

await cp(join(webRoot, "platforms", "tizen", "config.xml"), join(tizen, "config.xml"));
await cp(join(webRoot, "platforms", "webos", "appinfo.json"), join(webos, "appinfo.json"));
await cp(join(webRoot, "platforms", "webos", "icon.svg"), join(webos, "icon.svg"));
await cp(join(webRoot, "platforms", "webos", "index.html"), join(webos, "index.html"));

console.log("Built:");
console.log(`  ${browser}`);
console.log(`  ${tizen}`);
console.log(`  ${webos}`);
