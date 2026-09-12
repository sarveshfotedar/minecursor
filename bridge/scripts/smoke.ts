import { Agent } from "@cursor/sdk";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

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

loadDotEnv(resolve(process.cwd(), ".env"));

const apiKey = process.env.CURSOR_API_KEY;
if (!apiKey) {
  console.error("Missing CURSOR_API_KEY");
  process.exit(1);
}

const workspace = resolve(process.env.CURSOR_WORKSPACE ?? process.cwd());
console.log("workspace:", workspace);
console.log("key present:", true);

const result = await Agent.prompt("Reply with exactly the word pong and nothing else.", {
  apiKey,
  model: { id: process.env.CURSOR_MODEL ?? "composer-2.5" },
  local: { cwd: workspace }
});

console.log("status:", result.status);
console.log("result:", (result.result ?? "").slice(0, 200));
if (result.error) {
  console.error("error:", result.error.message);
  process.exit(1);
}
