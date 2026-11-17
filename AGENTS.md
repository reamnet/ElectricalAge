# AGENTS.md — Electrical Age (eln) 26.3 port

This repo ports Electrical Age (mod id `eln`) to Minecraft 26.3 / NeoForge 26.3 on Java 25 + Kotlin.
Read `CLAUDE.md` and `docs/` (esp. `docs/testing.md`) for the broader build/test conventions before
touching mod code.

This file adds **model-independent Minecraft dev + gameplay test tooling** (`tools/mctest/`) that any
coding agent can use with a plain terminal — no OpenAI/Codex plugins, no MCP, no GUI required. It is
designed so a local Ollama model (e.g. Qwen 27B) can run the same checks.

See `MCTEST_PROGRESS.md` for the live status, verification log, and the current mod-load blocker.

---
## Quick start

```bash
cd /home/matthew/projects/electrical-age-port/ElectricalAge
source /tmp/eln-port-java-env.sh          # JDK25 (target) + JDK21 (source) + gradle cache
alias mctest='python3 tools/mctest/mctest'

mctest doctor                             # env self-check (first thing to run)
mctest server start                       # launch ./gradlew runServer; waits for RCON
mctest server exec version                # run a server command over RCON
mctest lab netdump                        # run /elnlab netdump; reports artifact path
mctest sim run                            # headless GameTest harness (exit code = #failed)
mctest logs errors --which server         # inspect the server log
```

`tools/mctest/mctest` is a single stdlib-only Python file (no pip deps). State lives in
`tools/mctest/.state/`; screenshots in `tools/mctest/shots/` (both git-ignored).

---
## Environment (required before most commands)
- `source /tmp/eln-port-java-env.sh` sets:
  - `JAVA_HOME`/`JDK25` = JDK 25 (`/home/matthew/jdks/jdk-25.0.5+8`), prepended to PATH
  - `JDK21` = JDK 21 (`/home/matthew/jdks/jdk-21.0.13+9`)
  - `GRADLE_USER_HOME` = `/tmp/eln-port-gradle`
  - Plain `java` is NOT on PATH until you source this.
- `python3` = 3.14.4 (mctest is stdlib-only).
- GUI: `DISPLAY=:0` (real 1920x1080 cloud desktop). `xdotool` + `scrot` are on PATH.
  (Xvfb headless is NOT usable in this container — see "Not replicable".)
- Dev server RCON (from `run/server.properties`): `enable-rcon=true`, port `25575`,
  password `eln-local-dev-only`, game port `26035`, `online-mode=false`.
- In-game test hook: `src/main/java/mods/eln/gametest/LabCommands.java` (`/elnlab`, op 2).

## RCON target overrides (test against a stand-in server)
mctest reads RCON connection from `run/server.properties`, overridable via env:
- `MCTEST_RCON_HOST`, `MCTEST_RCON_PORT`, `MCTEST_RCON_PASSWORD`
Example: point at a reference RCON server on port 26677:
```bash
MCTEST_RCON_PORT=26677 mctest server exec version
```

---
## Command reference

### `doctor`
Environment self-check. Emits JSON: `ok` + a list of HARD checks (JDK25, JDK21, gradlew, python3,
DISPLAY, xdotool, scrot, rcon enabled, rcon port, server port) and SOFT checks (live RCON, live
minecraft window, Xvfb headless). All HARD checks must pass to run the rest.

### `server` — dev server over RCON (protocol 3)
- `server start [--server-addr 127.0.0.1] [--server-port 26035] [--rcon-port 25575] [--timeout 180]`
  Launches `./gradlew runServer` in its own process group; streams to
  `tools/mctest/.state/logs/server.log`; blocks until RCON accepts an auth handshake.
- `server stop [--grace 8]` — SIGTERM the process group; SIGKILL after grace.
- `server status` — JSON: `running` (managed proc), `rcon_live` (auth succeeds), `log` path.
- `server exec <cmd> [--json]` — run one server command via RCON (e.g. `version`, `list`, `/elnlab ...`).

### `client` — dev client via xdotool/scrot on DISPLAY :0
- `client start [--server-addr ...] [--username mctest] [--timeout 180]` — seeds `servers.dat`,
  launches `./gradlew runClient`, waits for the Minecraft window.
- `client stop` — kill the client process group.
- `client status` — JSON: `running`, `window` (xdotool window id), `server`.
- `client chat <message>` — type into the focused window + Enter (T chat box path).
- `client key <keys...>` — send keys (e.g. `client key F`, `client key f2`).
- `client type <text>` — type raw text (no Enter).
- `client click [x] [y] [--button 1]` — left/right/middle click at or current position.
- `client screenshot [name] [--mode in-game|window|auto] [--outdir DIR]` —
  - `in-game`: F2 then capture; `window`: capture the Minecraft window; `auto`: window then in-game.
  - Writes PNG to `tools/mctest/shots/`.
- `client window` — print the detected Minecraft window id + geometry.

### `lab` — in-game `/elnlab` subcommand via RCON
- `lab <args...>` — runs `/<elnlab> <args>` on the server and prints any artifact path it detects
  under `run/` (e.g. `lab netdump` -> `run/eln-netdump/<stamp>.json`).
  Useful subcommands: `gallery, lamps, lampmenu, voxelgallery, burn, cables, devicevolts,
  netdump, remove, reconnect, heat, tilted, threelogger, fluidtrace, inspect, machines`.

### `sim` — headless GameTest harness
- `sim run [--case eln:x] [--bench result.json] [--label NAME] [--convert] [--no-rth] [--timeout 3600]`
  Wraps `./gradlew runGameTestServer ...`; the Gradle exit code equals the number of failed tests.
  Logs to `tools/mctest/.state/logs/sim.log`. (Needs the server to boot — see blocker.)

### `logs` — inspect captured logs
- `logs list` — list files under `tools/mctest/.state/logs/`.
- `logs tail --which server|client|sim [--tail N]` — last N lines.
- `logs grep --which ... --grep PATTERN` — grep (note the flag is `--grep`, not `-p`).
- `logs errors --which ...` — ERROR/FATAL lines only.

### `regression` — full validation pipeline
Runs, in order: `build` -> `:test` -> `runGameTestServer` -> `runClientData` (datagen idempotence)
-> golden-delta check -> screenshot. Intended as the "prove nothing regressed" gate before pushing.
(Blocked end-to-end by the current mod-load blocker; see below.)

### `http` — local JSON control plane
`http [--port 8099] [--bind 127.0.0.1]` starts a stdlib `ThreadingHTTPServer`:
- `GET  /status`  -> `{"server":..,"server_rcon_live":..,"client":..}`
- `GET  /logs`    -> `{"logs":[...]}`
- `POST /` with a JSON body carrying an `action` field:
  - `{"action":"exec","command":"version"}` -> `{"response":"..."}`
  - `{"action":"chat","message":"hi"}`       -> `{"sent":"hi"}`
  - `{"action":"screenshot"}`                 -> `{"screenshot":"<path or null>"}`
  - `{"action":"tail","which":"server","n":50}` -> `{"which":"server","tail":"..."}`

---
## Mock/stand-in testing vs REAL Minecraft gameplay — IMPORTANT
- VERIFIED working now (no live game needed): `doctor`, RCON protocol (vs a stand-in server),
  `logs *`, `http *`, `scrot` desktop capture, `xdotool` presence.
- PLUMBING-VERIFIED ONLY (logic + wiring correct, but not exercised against a live server/client
  because the in-tree WIP currently fails at mod-load): `server start/stop`, `client start/stop/
  chat/key/click/type/screenshot-in-game`, `lab` artifact capture, `sim run`, `regression`.
- For those, "verified" means the tooling talks correctly to a **reference RCON stand-in**, which is
  a weaker claim than "validated in a running game". Do not report them as gameplay-validated.

## Current blocker (blocks ALL real server/client boot)
Uncommitted WIP (12-circuit breaker panel) fails at construct-mod:
  `IllegalArgumentException: Required value was null`
  ServiceWallDescriptor.kt:16 (`requireNotNull(Eln.obj.getObj("twelvecircuitpanel"))`)
  <- BreakerPanelDescriptor (BreakerPanelClient.kt:15)
  <- ElnDeviceRegistry.registerBatch1 (ElnDeviceRegistry.java:119)
Until that WIP is fixed, `./gradlew runServer` and `runClient` cannot start, so no real in-game
testing is possible. Do NOT fix that WIP as part of test-infrastructure work; preserve it.

## Not replicable independently (do not try to force)
- Xvfb headless rendering in THIS container (`/tmp/.X11-unix` read-only bind; no sudo; no xkbcomp).
  In-game capture depends on the real `DISPLAY :0` desktop.
- Hardware GPU / audio parity (needs a real GPU + audio device).
- Subjective human visual/perceptual QA (screenshots are captured programmatically; the judgment is human).
- Simultaneous multi-client stress (open item from the port notes).
- Online-mode / real-account auth (offline mode only).

## Rules for agents
- Preserve all uncommitted porting WIP. Test-infrastructure commits must contain ONLY
  `tools/mctest/`, `AGENTS.md`, `MCTEST_PROGRESS.md`, and the `.gitignore` additions.
- Do not change pinned Gradle/wrapper/dependency config. Do not weaken goldens, assertions, or skips.
- Do not compile changed main classes while a test/client JVM is using the same build outputs.
- `:test` (not bare `test`) for root-only test filters (bare `test` also reaches `:cad:test`).
- Do not push without explicit user permission.
