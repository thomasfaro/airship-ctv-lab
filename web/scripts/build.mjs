import { cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const webRoot = resolve(here, "..");
const repoRoot = resolve(webRoot, "..");
const source = join(webRoot, "src");
const output = join(webRoot, "dist");

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

const properties = await readProperties(
  join(repoRoot, "config", "airship.local.properties"),
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

if (!properties["airship.webToken"] || !properties["airship.webVapidPublicKey"]) {
  console.warn(
    "\nAirship Web credentials are missing. Add airship.webToken and " +
      "airship.webVapidPublicKey to config/airship.local.properties.",
  );
}
