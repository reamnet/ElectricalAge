# MCTEST_PROGRESS — Model-independent Minecraft test infrastructure

Repo root: /home/matthew/projects/electrical-age-port/ElectricalAge
Branch: port/forkpatch-3.0.77-26.3 (draft PR #1 — keep title/base/draft)
Goal: standalone shell/Python/local-HTTP tools so a local Ollama Qwen 27B coding agent can
run the same dev + gameplay tests WITHOUT any OpenAI-specific plugins. Location: tools/mctest/.

Status legend: [x] done+verified  [~] in progress  [!] blocked  [?] needs info

Last updated: 2026-11-17 (infra completed + verified against a reference RCON server)

------------------------------------------------------------------------------
## TL;DR
- `tools/mctest/mctest` (single stdlib-only Python file, ~1083 lines) is COMPLETE and
  compiles cleanly. All 8 top-level commands exist:
  doctor | server | client | lab | sim | logs | regression | http
- RCON client (Source RCON protocol 3) was buggy and has been FIXED + verified:
  auth request type 3, success id==0 / fail id==-1, exec request type 2, _send/_recv
  byte-exact. (Previously never exercised because the real server never loaded.)
- Verified this session (actual results in "Per-component verification log"):
  doctor, server exec/status (vs ref RCON), lab routing+artifact discovery,
  logs list/tail/grep/errors, http /status+/logs+POST exec/tail,
  scrot desktop capture, xdotool present.
- BLOCKER (not infra — do NOT fix): the in-tree WIP fails at mod-load, so a real
  server/client cannot start in this tree. All in-game (client/lab-vs-real/sim/regression)
  paths are therefore verified only against the reference RCON server, NOT real gameplay.
- AGENTS.md written at repo root. .gitignore updated (tools/mctest/.state/, shots/).
- NOT pushed (awaiting user permission).

------------------------------------------------------------------------------
## Verified environment
- Java env: `source /tmp/eln-port-java-env.sh`
    JDK25 = /home/matthew/jdks/jdk-25.0.5+8   (target, prepended to PATH)
    JDK21 = /home/matthew/jdks/jdk-21.0.13+9  (source)
    GRADLE_USER_HOME = /tmp/eln-port-gradle
    NOTE: plain `java` is NOT on PATH until this script is sourced.
- python3 = 3.14.4 (target is stdlib-only; no pip deps for mctest).
- DISPLAY=:0 (1920x1080 cloud desktop). xdotool=/usr/bin/xdotool, scrot=/usr/bin/scrot.
- Server RCON ENABLED in run/server.properties:
    enable-rcon=true, rcon.password=eln-local-dev-only, rcon.port=25575,
    server-port=26035, online-mode=false. (Drivable by raw-TCP Source RCON proto 3.)
- In-game hook: src/main/java/mods/eln/gametest/LabCommands.java (~818 lines, `/elnlab`,
  op level 2, registered unconditionally). Subcommands include: gallery, lamps,
  lampmenu(4116..4499), voxelgallery, burn, cables, devicevolts, netdump, remove,
  reconnect, heat, tilted, threelogger, fluidtrace, inspect, machines, ...
- Lab artifacts (server dir = run/): eln-netdump/<stamp>.json + .falstad.txt,
  eln-lighting-gallery.json, eln-voxelcables.txt.
- mctest state: tools/mctest/.state/ (pids, logs). Screenshots: tools/mctest/shots/.

------------------------------------------------------------------------------
## EXACT ELECTRICAL AGE MOD-LOAD BLOCKER (root cause, from latest crash report)
run/crash-reports/crash-2026-11-17_00.46.55-fml.txt
  java.lang.IllegalArgumentException: Required value was null.
    at mods.eln.utility.ServiceWallDescriptor.<init>(ServiceWallDescriptor.kt:16)
    at mods.eln.utility.BreakerPanelDescriptor.<init>(BreakerPanelClient.kt:15)
    at mods.eln.registry.ElnDeviceRegistry.registerBatch1(ElnDeviceRegistry.java:119)
    at mods.eln.Eln.<init>(Eln.java:318)

Diagnosis:
  - ServiceWallDescriptor.kt:16 = `requireNotNull(Eln.obj.getObj(model))` where
    model="twelvecircuitpanel" (passed by BreakerPanelDescriptor, BreakerPanelClient.kt:15).
    `Eln.obj.getObj("twelvecircuitpanel")` is null at mod-instance construction time
    (model object not yet resvable/registered when the descriptor is constructed).
  - This is the UNCOMMITTED WIP (12-circuit breaker panel: BreakerPanel*.kt,
    ServiceWallDescriptor.kt, eln-ids.json, ElnDeviceRegistry.java:119 wiring,
    assets/eln/model/twelvecircuitpanel/). It is NOT a mctest/infra defect.
  - Consequence: `./gradlew runServer` and any client start FAIL at construct-mod, so
    real in-game testing cannot run in this tree until that WIP is fixed.
  - Instruction honored: do NOT fix this WIP as part of infra; preserve it.

------------------------------------------------------------------------------
## Components (final status)
1. [x] mctest CLI scaffold + stdlib RCON client (protocol 3) — FIXED + verified.
2. [x] doctor (env self-check) — verified (all hard checks pass).
3. [x] server start|stop|status|exec — plumbing verified; start/stop vs REAL server
       [!] BLOCKED by WIP mod-load (gradle runServer crashes at construct-mod).
       exec/status verified against reference RCON server.
4. [x] client start|stop|status|chat|key|type|click|screenshot|window —
       helpers present (xdotool/scrot on :0). scrot desktop capture verified.
       [!] In-game client paths BLOCKED by WIP mod-load (real client cannot start).
       (No xterm; Xvfb headless unusable in this container — see non-replicable.)
5. [x] lab <elnlab args...> — verified routes `/elnlab ...` through RCON and reports
       the expected artifact path (run/eln-lighting-gallery.json). Real artifact
       capture [!] BLOCKED by WIP (no live server).
6. [x] sim run [--case eln:x] — gradle wrapper invocation + exit-code wiring present.
       [!] BLOCKED by WIP (runGameTestServer crashes at mod-load).
7. [x] logs tail|grep|errors|list [--which server|client|sim] — verified.
       NOTE: grep flag is `--grep PATTERN` (not -p).
8. [x] regression — pipeline wiring present (build -> :test -> runGameTestServer ->
       datagen idempotence -> golden-delta -> screenshot). [!] BLOCKED by WIP.
9. [x] http (stdlib ThreadingHTTPServer JSON control plane) — verified.
       GET /status, GET /logs, POST {"action":"exec"|"chat"|"screenshot"|"tail"}.
       screenshot returns null with no target window (expected under WIP blocker).
10.[x] MCTEST_PROGRESS.md (this file).
11.[x] AGENTS.md (repo root) — quick-start, full command ref, env, non-replicable list.
12.[x] .gitignore entries for tools/mctest/.state/ and tools/mctest/shots/.
13.[x] Local commit of infra ONLY (WIP excluded). [?] PUSH — awaiting user permission.

------------------------------------------------------------------------------
## MOCK-SERVER TESTING vs REAL MINECRAFT GAMEPLAY — read this
- VERIFIED REAL (works now): env self-check (doctor), RCON protocol against a
  stand-in server, log inspection, http control plane, desktop screenshot capture
  (scrot), xdotool presence.
- VERIFIED BY CONSTRUCTION / PLUMBING ONLY (logic + wiring correct, not against a live
  server/client because WIP blocks server load): server start/stop, client
  start/stop/chat/key/click/type/screenshot-in-game, lab artifact capture, sim run,
  regression pipeline.
- NOT VERIFIABLE HERE AT ALL (see "Non-replicable"): real gameplay perception, HW
  GPU/audio, Xvfb headless in this container, online-mode auth, multi-client stress.
- To do REAL in-game testing you MUST first fix the WIP mod-load blocker above so the
  server (and hence client) can boot. Until then, "verified" for those paths means
  "tooling plumbing verified against the reference RCON server", which is a different
  and weaker claim than "validated in a running game".

------------------------------------------------------------------------------
## Per-component verification log (actual results, 2026-11-17)
- doctor:
    `python3 tools/mctest/mctest doctor` -> {"ok": true}; all HARD checks pass
    (JDK25, JDK21, gradlew, python3, DISPLAY :0, xdotool, scrot, rcon enabled,
     rcon port 25575, server port 26035). SOFT (informational): server RCON live =
     not running; minecraft window = not running; Xvfb headless = not usable.
- RCON client (FIXED): against /tmp/refrcon.py (canonical proto-3 stand-in),
    `server exec version` -> "Bukkit version 26.3"; `list`/`time`/empty `say` OK;
    `--json` OK; wrong password -> clean "RCON auth failed (bad password)" (no hang).
- server status (vs ref RCON): {"running": false, "rcon_live": true, ...}.
- lab: `lab gallery` -> routed /elnlab gallery; reported artifact path
    run/eln-lighting-gallery.json (file present from prior runs).
- logs:
    `logs list` -> 3 files (proc-test.log, sim.log, server.log).
    `logs tail --which server` -> shows last lines (BUILD FAILED tail).
    `logs grep --which server --grep "Required value"` -> 4 hits (the blocker).
    `logs grep --which server --grep "Failed to create"` -> FMLModContainer line.
    `logs errors --which server` -> FATAL ModLoadingCrash + chain to ServiceWallDescriptor.
- http (port 18099):
    GET  /status -> {"server": false, "server_rcon_live": true, "client": false}
    GET  /logs   -> {3 log paths}
    POST {"action":"exec","command":"version"} -> {"response": "Bukkit version 26.3"}
    POST {"action":"tail","which":"server","n":3} -> {"which":"server","tail":"..."}
    POST {"action":"screenshot"} -> {"screenshot": null} (no target window; expected)
- screenshot (desktop): scrot on DISPLAY :0 produced a full-desktop PNG earlier
    (tools/mctest/shots/primitive-test.png, 5332x3000). Confirms X capture path works.
- py_compile tools/mctest/mctest -> OK. mctest --help -> all subcommands listed.

------------------------------------------------------------------------------
## Non-replicable independently (document, do not try to force)
- Xvfb headless rendering in THIS container (/tmp/.X11-unix is a read-only bind mount;
  no sudo; xkbcomp missing). In-game capture relies on the real DISPLAY :0 desktop.
- Hardware GPU / audio parity (needs real GPU + audio device).
- Real human visual / perceptual QA (screenshots are captured programmatically, but
  subjective judgment of "does it look right" is human-only).
- Simultaneous multi-client stress (open item from the port notes).
- Full online-mode / real-account auth (offline mode only; session auth not exercised).

------------------------------------------------------------------------------
## REMAINING WORK TO DO REAL IN-GAME TESTING
Order of operations:
1. Fix the WIP mod-load blocker (12-circuit breaker panel WIP):
     - Make `Eln.obj.getObj("twelvecircuitpanel")` non-null at descriptor construction,
       e.g. resolve the model lazily (BreakerPanelDescriptor already has a `lazy` model),
       or register the model object before ElnDeviceRegistry.registerBatch1 (line 119),
       or move the BreakerPanelDescriptor registration to after model objects are available.
   This is the single thing that currently prevents ANY server/client from booting.
2. `./gradlew runServer` boots cleanly (RCON on 25575). Then:
     `python3 tools/mctest/mctest server start` (blocks until RCON ready).
     `python3 tools/mctest/mctest server exec version` -> real version.
     `python3 tools/mctest/mctest lab gallery` (and other /elnlab subcommands) ->
     capture run/eln-*.json artifacts.
     `python3 tools/mctest/mctest client start --username mctest` -> connect to 26035.
     `python3 tools/mctest/mctest client screenshot in-game` -> visual capture.
     `python3 tools/mctest/mctest client chat "hello"` / `key F` / `click x y` -> interact.
3. Headless regression: `python3 tools/mctest/mctest sim run` (runGameTestServer;
   exit code = number of failed tests) and full `regression` pipeline once server boots.
4. Electrical-simulation specifics: use `lab netdump` (eln-netdump/<stamp>.json +
   falstad) and `lab devicevolts` / `lab burn` / `lab cables` against a live server to
   assert grid voltages/flows. These are the true "electrical simulation" tests and
   REQUIRE a running server.

------------------------------------------------------------------------------
## Uncommitted WIP to PRESERVE (do NOT commit as part of infra)
docs/forkpatch-port/portable-carry.md, docs/forkpatch-port/requested-systems-plan.md,
src/.../gametest/cases/ForkPatchBreakerPanelCases.kt, .../ForkPatchPortableCarryCases.kt,
src/.../utility/BreakerPanel*.kt, WirelessPowerChannels.kt, ServiceWallDescriptor.kt,
assets/eln/model/twelvecircuitpanel/, eln-ids.json, ElnDeviceRegistry.java, Wp9aContent.java,
plus ~28 other modified java/kt files. (See `git status --short`.)

## Infra files to commit (ONLY these)
tools/mctest/mctest
AGENTS.md
MCTEST_PROGRESS.md
.gitignore  (mctest state/shots additions)
