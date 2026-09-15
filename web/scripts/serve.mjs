import { createReadStream, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, join, normalize, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(fileURLToPath(new URL("../dist/browser/", import.meta.url)));
const port = Number(process.env.PORT || 4173);
const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
};

createServer((request, response) => {
  const pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
  const relative = normalize(pathname).replace(/^(\.\.(\/|\\|$))+/, "");
  let file = join(root, relative === "/" ? "index.html" : relative);

  try {
    if (statSync(file).isDirectory()) file = join(file, "index.html");
    response.writeHead(200, {
      "Content-Type": contentTypes[extname(file)] || "application/octet-stream",
      "Cache-Control": "no-store",
    });
    createReadStream(file).pipe(response);
  } catch {
    // /play/<film-id> is a client-side route, matching the Netlify rewrite.
    if (pathname.startsWith("/play/")) {
      response.writeHead(200, {
        "Content-Type": contentTypes[".html"],
        "Cache-Control": "no-store",
      });
      createReadStream(join(root, "index.html")).pipe(response);
      return;
    }
    response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Not found");
  }
}).listen(port, "0.0.0.0", () => {
  console.log(`CTV Lab Web: http://localhost:${port}`);
  console.log("Run npm run build after changing source or credentials.");
});
