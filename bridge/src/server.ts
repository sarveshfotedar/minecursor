import { readFileSync, existsSync } from "node:fs";
import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { resolve } from "node:path";
import { CursorSession } from "./session.js";

loadDotEnv(resolve(process.cwd(), ".env"));

const HOST = process.env.CURSOR_LINK_HOST ?? "127.0.0.1";
const PORT = Number(process.env.CURSOR_LINK_PORT ?? 43147);
const WORKSPACE = resolve(process.env.CURSOR_WORKSPACE ?? process.cwd());
const API_KEY = process.env.CURSOR_API_KEY;
const MODEL = process.env.CURSOR_MODEL ?? "composer-2.5";

const session = new CursorSession(API_KEY, WORKSPACE, MODEL);
let promptQueue: Promise<void> = Promise.resolve();

const server = createServer(async (request, response) => {
  try {
    await handle(request, response);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    send(response, 500, { error: message });
  }
});

async function handle(request: IncomingMessage, response: ServerResponse): Promise<void> {
  const url = new URL(request.url ?? "/", `http://${HOST}:${PORT}`);

  if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/health")) {
    const snap = session.snapshot();
    send(response, 200, {
      ok: true,
      service: "cursor-link-bridge",
      runtime: "local",
      host: HOST,
      port: PORT,
      workspace: snap.workspace,
      hasApiKey: snap.hasApiKey,
      status: snap.status
    });
    return;
  }

  if (request.method === "GET" && url.pathname === "/v1/session") {
    send(response, 200, session.snapshot());
    return;
  }

  if (request.method === "POST" && url.pathname === "/v1/prompt") {
    const body = await readJson(request);
    const text = typeof body.text === "string" ? body.text : "";
    if (!text.trim()) {
      send(response, 400, { error: "Type a message first." });
      return;
    }
    if (!session.snapshot().hasApiKey) {
      send(response, 400, { error: "The helper has no CURSOR_API_KEY. Add one to bridge/.env and restart it." });
      return;
    }
    promptQueue = promptQueue.then(() => session.prompt(text)).catch((error) => {
      console.error(error);
    });
    // Do not wait for the agent to finish — Minecraft polls /v1/session.
    send(response, 202, { accepted: true });
    return;
  }

  if (request.method === "POST" && url.pathname === "/v1/cancel") {
    await session.cancel();
    send(response, 200, { cancelled: true });
    return;
  }

  send(response, 404, { error: "Not found" });
}

function readJson(request: IncomingMessage): Promise<Record<string, unknown>> {
  return new Promise((resolvePromise, reject) => {
    const chunks: Buffer[] = [];
    request.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
    request.on("end", () => {
      if (chunks.length === 0) {
        resolvePromise({});
        return;
      }
      try {
        resolvePromise(JSON.parse(Buffer.concat(chunks).toString("utf8")) as Record<string, unknown>);
      } catch (error) {
        reject(error);
      }
    });
    request.on("error", reject);
  });
}

function send(response: ServerResponse, status: number, body: unknown): void {
  const json = JSON.stringify(body);
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(json),
    "Cache-Control": "no-store"
  });
  response.end(json);
}

function loadDotEnv(path: string): void {
  if (!existsSync(path)) {
    return;
  }
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#") || !trimmed.includes("=")) {
      continue;
    }
    const eq = trimmed.indexOf("=");
    const key = trimmed.slice(0, eq).trim();
    const value = trimmed.slice(eq + 1).trim().replace(/^['"]|['"]$/g, "");
    if (key && process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

server.listen(PORT, HOST, () => {
  console.log(`Cursor Link helper listening on http://${HOST}:${PORT}`);
  console.log(`Workspace: ${WORKSPACE}`);
  console.log(API_KEY ? "API key: present" : "API key: missing — set CURSOR_API_KEY");
  console.log("This helper only accepts connections from this computer.");
});
