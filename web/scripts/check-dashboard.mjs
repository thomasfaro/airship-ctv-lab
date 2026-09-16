import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { join } from "node:path";

const output = join(import.meta.dirname, "..", "dist", "browser");
const [configSource, html] = await Promise.all([
  readFile(join(output, "airship-config.js"), "utf8"),
  readFile(join(output, "index.html"), "utf8"),
]);

const serialized = configSource.match(/^window\.CTVLAB_CONFIG = (.*);\s*$/s);
assert.ok(serialized, "generated runtime configuration is readable");
const config = JSON.parse(serialized[1]);

assert.equal(config.liveSiteUrl, "https://airship-ctv-web-lab.netlify.app");
assert.deepEqual(
  config.embeddedSlots.map(({ embeddedId }) => embeddedId),
  ["home_top", "home_banner"],
);
assert.ok(config.availableUrls.includes(`${config.liveSiteUrl}/play/<film-id>`));
assert.ok(config.availableUrls.includes(`${config.liveSiteUrl}/?play=<film-id>`));
assert.ok(config.deepLinks.some((value) => value.includes("native-only")));

[
  "lab-live-site",
  "lab-embedded-ids",
  "lab-custom-components",
  "lab-available-urls",
  "lab-deep-links",
].forEach((id) => assert.match(html, new RegExp(`id="${id}"`)));

console.log("Lab dashboard inventory verified.");
