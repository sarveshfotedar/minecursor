# Cursor Link

Talk to Cursor from inside Minecraft.

This is a **client-only** Fabric mod plus a small helper that runs on your computer. Other people on a plugin server cannot see the screen, cannot send your prompts, and do not need the mod installed.

## What we checked first

Nobody has already shipped “Minecraft as a Cursor chat window.”

What *does* exist is the other direction, or a different product:

- **Cursor → Minecraft:** MCP mods and plugins let Cursor control a world. That is the reverse of this project.
- **[MC-CC](https://github.com/pisanvs/mc-cc):** closest cousin. It is a Fabric screen for talking to **Claude Code** through **herdr**, not Cursor.
- **“Cursor for Minecraft” / Steve:** an AI that *plays* Minecraft. The name is marketing, not a Cursor IDE link.
- **Minecraft Cursor (fishstiz):** changes the mouse pointer. Unrelated.

So this repo is the first slice of **Minecraft → Cursor**.

## How it works

```
You (in Minecraft)  →  Cursor Link screen
                         │
                         │ HTTP on 127.0.0.1 only
                         ▼
                 Helper on this PC  →  Cursor agent (local)
                         │
                         ▼
                 Replies stream back into the same screen
```

The Minecraft server never sees the messages. Cloud Agents are a later option; this first version stays on your computer.

## What you need

- Minecraft **Java Edition 26.2**
- [Fabric Loader](https://fabricmc.net/use/installer/) for 26.2
- [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2
- [Node.js 22+](https://nodejs.org/) for the helper
- A Cursor API key from [cursor.com/dashboard](https://cursor.com/dashboard/api)
- The project folder you want Cursor to work on

If you play a different Minecraft version, say so and we can retarget the mod.

## 1. Start the helper

On the same computer that runs Minecraft:

```bash
cd bridge
cp .env.example .env
```

Edit `.env`:

- `CURSOR_API_KEY` — your key from the Cursor dashboard
- `CURSOR_WORKSPACE` — the full path of the project Cursor should edit

Then:

```bash
npm install
npm start
```

Leave that window open. You should see `listening on http://127.0.0.1:43147`.

The helper only accepts connections from this computer.

## 2. Install the mod

1. Build it (needs Java 25, which Minecraft 26.2 also uses):

   ```bash
   cd fabric
   ./gradlew build
   ```

2. Copy `fabric/build/libs/cursorlink-0.1.0.jar` into your `.minecraft/mods` folder.
3. Also put **Fabric API** in that folder.
4. Launch the 26.2 Fabric profile.

## 3. Use it in-game

1. Join singleplayer or any multiplayer server (plugin servers are fine).
2. Press **K** (rebind in Options → Controls → Cursor Link).
3. Type a message and press **Send** or Enter.
4. Watch the reply stream in the same window.

A small “Cursor is working...” tag also appears on the HUD while a job is running.

Only you see this. It is not a server command and it is not sent as public chat.

## Config

The first launch writes `.minecraft/config/cursorlink.json`:

```json
{
  "bridgeUrl": "http://127.0.0.1:43147"
}
```

Leave that as-is unless you changed the helper port.

## Empty / error states

| What you see | What it means |
| --- | --- |
| “Helper is not running” | Start `npm start` in `bridge/` on this PC |
| “no Cursor API key” | Put `CURSOR_API_KEY` in `bridge/.env` and restart the helper |
| “Cursor is working...” | A local agent run is in progress |
| A red error line | The helper or Cursor returned a message; read it on the screen |

## Later: cloud and graphics

The helper already has a reserved `graphic` event type so later screens, maps, or world overlays can share the same pipe. Cloud Agents can use the same `/v1/prompt` route later without changing the Minecraft screen.

## Not affiliated

Not affiliated with Mojang, Microsoft, or Cursor. You need your own Minecraft and Cursor accounts.
