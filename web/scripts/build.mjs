import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(here, "..");
const repoRoot = resolve(webRoot, "..");
const source = join(webRoot, "src");
const output = join(webRoot, "dist");

const PLACEHOLDER_VALUES = new Set([
  "",
  "YOUR_APP_KEY",
  "YOUR_APP_SECRET",
  "YOUR_WEB_TOKEN",
  "YOUR_WEB_VAPID_PUBLIC_KEY",
]);

function envValue(...names) {
  for (const name of names) {
    const value = process.env[name];
    if (value && !PLACEHOLDER_VALUES.has(value.trim())) return value.trim();
  }
  return "";
}

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

// Local builds read config/airship.local.properties. Netlify has no copy of that
// gitignored file, so the same keys come from environment variables instead.
// airship.appSecret is dropped even if present: it belongs to the mobile apps.
function webCredentials(fileProperties) {
  const properties = { ...fileProperties };
  delete properties["airship.appSecret"];

  const filled = {
    "airship.appKey":
      fileValue(properties, "airship.appKey") || envValue("AIRSHIP_APP_KEY", "airship.appKey"),
    "airship.site":
      fileValue(properties, "airship.site") || envValue("AIRSHIP_SITE", "airship.site") || "eu",
    "airship.webToken":
      fileValue(properties, "airship.webToken") || envValue("AIRSHIP_WEB_TOKEN", "airship.webToken"),
    "airship.webVapidPublicKey":
      fileValue(properties, "airship.webVapidPublicKey") ||
      envValue("AIRSHIP_WEB_VAPID_PUBLIC_KEY", "airship.webVapidPublicKey"),
    "airship.webDefaultIcon":
      fileValue(properties, "airship.webDefaultIcon") ||
      envValue("AIRSHIP_WEB_DEFAULT_ICON", "airship.webDefaultIcon"),
    "airship.webDefaultTitle":
      fileValue(properties, "airship.webDefaultTitle") ||
      envValue("AIRSHIP_WEB_DEFAULT_TITLE", "airship.webDefaultTitle"),
    "airship.webDefaultActionURL":
      fileValue(properties, "airship.webDefaultActionURL") ||
      envValue("AIRSHIP_WEB_DEFAULT_ACTION_URL", "airship.webDefaultActionURL"),
  };

  const missing = ["airship.appKey", "airship.webToken", "airship.webVapidPublicKey"].filter(
    (key) => !filled[key] || PLACEHOLDER_VALUES.has(filled[key]),
  );
  if (missing.length) {
    throw new Error(
      "Missing Airship Web credentials: " +
        missing.join(", ") +
        ". For a local build, set them in config/airship.local.properties. " +
        "For Netlify, set AIRSHIP_APP_KEY, AIRSHIP_WEB_TOKEN and " +
        "AIRSHIP_WEB_VAPID_PUBLIC_KEY from Training app → Settings → Channels → Web → Install SDK. " +
        "Do not set airship.appSecret: it is never copied into the HTML app.",
    );
  }

  return filled;
}

function runtimeConfig(properties, platform) {
  return `window.CTVLAB_CONFIG = ${JSON.stringify(
    {
      appKey: properties["airship.appKey"] ?? "",
      token: properties["airship.webToken"] ?? "",
      vapidPublicKey: properties["airship.webVapidPublicKey"] ?? "",
      site: properties["airship.site"] ?? "eu",
      platform,
      embeddedSlots: [
        { selector: "#airship-home-top", embeddedId: "home_top" },
        { selector: "#airship-home-banner", embeddedId: "home_banner" },
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

let properties;
try {
  properties = webCredentials(
    await readProperties(join(repoRoot, "config", "airship.local.properties")),
  );
} catch (error) {
  console.error(error.message || error);
  process.exit(1);
}

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
