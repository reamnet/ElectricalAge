# AWG upstream integration progress

## Current checkpoint
- Task: audit original upstream delta, integrate compatible updates and six reliability commits, verify without changing source worktrees.
- Integration branch: `local/awg-upstream-integration`.
- Integration worktree: `/home/matthew/projects/electrical-age-port/ElectricalAge-awg-upstream-integration`.
- Local AWG source: `feature/awg-voxel-cables` at `7b91ee88416f5e4410a8e58eff744cdced523342` (three commits ahead of origin at task start).
- Reliability source: `local/gpt-awg-reliability` at `573af9324604ebf0555f147061e08e7af977b8f2`.
- Original upstream URL: https://github.com/Electrical-Age/ElectricalAge (remote `upstream`, NOT fork origin).
- Fetched original upstream `develop`: `2be56a18c59d1dd129bdb81d7ebee32ac0eb4c71` (November 25, 2026).
- Fork remote: https://github.com/reamnet/ElectricalAge (`origin`). No push/merge into feature or develop authorized.
- Six reliability commits verified: `1e2e7291d`, `182adb885`, `11a7839cd`, `2817ed508`, `75468da24`, `573af9324`.
- Worktree inventory: 38 worktrees, statuses read with optional Git locks disabled; Qwen inert_canister worktree has existing edits to Wp12aContent.java and eln-ids.json, preserved. New integration tree is clean before this document. Inventory in `/tmp/awg-integration-worktree-inventory.json`.
- Last verified action: original upstream fetch and isolated branch/worktree creation, exit 0. Initial status scan overlapped new checkout; rechecked new tree after checkout to remove transient status artifacts.
- Current HEAD: `7b91ee884`; uncommitted: this document only.
- Tests: pending; previous reliability baseline 1,992/1,995 server tests with three historical failures, not assumed for this integration.

## Exact next steps
1. Read applicable instructions and build/test guidance in this worktree.
2. Compute ancestry and upstream-only delta; build per-commit integration matrix before mutation.
3. Integrate reliability history without duplicates, then compatible upstream groups with attribution and deliberate conflict resolutions.
4. Compile/focused verification per group; full root/AWG/fluid/shaft/GameTest and twice datagen after stable integration.
5. Record results, remaining limits and final branch commit. Do not push or update feature/develop.

## Ancestry and merge decision
- Merge base: `29b2d2200f4c8bd301493d3f541abb5faaf07c56`; local feature has 226 commits and original upstream 229 commits beyond it. `git cherry` finds no exact patch-equivalent upstream commits among the missing 229.
- A three-way ancestry merge is appropriate; unrelated-history merge or wholesale source replacement is unnecessary. Preflight reports 36 conflicted paths, including generated outputs. Preserve original upstream commit ancestry/attribution and adapt conflicts locally.
- Upstream changes include fluid finite-state recovery/time/energy accounting, asynchronous fluid scheduling, shared route spec/placer and join rules, battery lead-acid behavior, shaft component/tag recipes, screen/Janet/GUI fixes, Ground Radar and distillation devices. Source and commit inventories are in `/tmp/awg-upstream-{commits,shortlog,files}.txt`.
- Critical registry collision found BEFORE merge: upstream Radar Goggles uses shared_item@7740; port already uses @7740 for Motor Cowling. Preserve Motor Cowling and assign the new-to-port goggles an unused ID; verify every old port mapping remains identical.
- Upstream removes working legacy 48V/230V heat turbines. Defer that removal to preserve port features and historical saves; keep their registrations/classes/assets/sounds and existing golden comparison allowances while retaining new upstream schematic steam turbine icons.
- Upstream adds an optional Inventory Profiles Next compile-only dependency solely for an ignore annotation. Preserve dependency pins/config: defer that dependency/annotation, keep the unrelated screen fixes from the same commit. Upstream non-version test-run options may be retained.
- Historical golden JSON snapshots are untouched upstream; golden README/source annotations describe intentional upstream battery/new-device behavior. Preserve snapshot contents and do not add skips to hide failures.

## Preliminary integration matrix
| Area | Classification | Decision and checks |
| --- | --- | --- |
| Common history through 29b2d2200 | Already present | Keep shared ancestry; no duplicate replay |
| Six reliability commits | Safe to integrate | Merge exact history first; focused preview/AWG tests |
| Fluid finite-state recovery, tank volume/energy/layer accounting, broken junction deletion | Safe; additional testing | Integrate author chain through tip; unit/replay/pipe/tank GameTests |
| Fluid scheduling/budgets/quarantine and diagnostics | Safe; additional testing | Preserve author timing contracts, test background and long plants |
| Shared KitPlacer/KitSpec routing and join rules | Requires adaptation | Carry AWG cached preview/payment/rollback/tap behavior and unique worker generations into shared path |
| Shaft composition and plate-tag recipes | Requires adaptation | Retain ForkPatch additions; regenerate intended author recipe changes twice |
| Battery lead-acid curve and finite NBT reads | Safe; additional testing | Keep author physical model and tests, preserve save compatibility |
| Radar/still/coil/breather additions | Requires adaptation | Resolve goggles numeric collision; preserve old mappings and test new devices |
| Legacy heat turbine removal | Intentionally excluded | Keep functional port devices, registrations, sound and assets |
| Optional IPN dependency/annotation | Deferred | No pinned dependency changes; independent UI fixes still integrate |
| Existing Arc Furnace/ForkPatch/AWG content | Already local; protected | Preserve local deltas during auto-merges and compare registry/assets after datagen |
| Historical snapshots | Protected | No JSON changes or weakened tests |
