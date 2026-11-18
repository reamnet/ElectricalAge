# Build Minecraft 26.3 with the ForkPatch port

Use **`port/forkpatch-3.0.77-26.3`** in **reamnet/ElectricalAge**. This is the PR #1 branch containing the completed ForkPatch port work. Checking out only `ports/26.3` does not include that work while the PR remains unmerged.

## Sync status checked 2026-11-18

- ForkPatch source repository: `reamnet/ELN1.21.1ForkPatch`, branch `main`.
- Remote source HEAD and reviewed source baseline both equal `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.
- Local port HEAD and remote `origin/port/forkpatch-3.0.77-26.3` both equal `86ab755f65f9a7305e9417fae92ef59745e545ea` at inspection.
- The separate `origin/ports/26.3` base has two commits not in the port branch: `067b6d8e` (fluid-container Jade contents) and `6fc512db` (liquid quantities in mB). These have **not** been merged by this documentation task.

Thus the source baseline matches the latest source `main` checked on this date, but the port is not fully feature-complete, nor synchronized with every newer base-branch commit. See [remaining item inventory](unported-items.md), [remaining work](remaining-work.md), and [cable parity audit](cable-source-parity-audit.md). A build includes all completed work committed on the selected branch, not the items still awaiting implementation.

## Fresh checkout

Install Git and complete JDK installations for **Java 25 and Java 21**. Initial dependency resolution needs internet access. Use the committed Gradle wrapper; no system Gradle installation is required.

```bash
git clone --branch port/forkpatch-3.0.77-26.3 --single-branch \
  https://github.com/reamnet/ElectricalAge.git
cd ElectricalAge
```

For an existing checkout, inspect `git status` first and preserve any local edits. Once they are committed or otherwise safely preserved:

```bash
git fetch origin
git switch port/forkpatch-3.0.77-26.3
git pull --ff-only origin port/forkpatch-3.0.77-26.3
```

A failed fast-forward means local and remote history need inspection; do not discard local commits to force an update.

## Java environment

On the existing development machine:

```bash
source /tmp/eln-port-java-env.sh
```

On another Linux/macOS machine, replace these paths with actual JDK home directories (on macOS these generally end in `Contents/Home`):

```bash
export JDK25=/absolute/path/to/jdk-25
export JDK21=/absolute/path/to/jdk-21
export JAVA_HOME="$JDK25"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
"$JDK21/bin/java" -version
```

Java 25 runs Gradle and the target mod; Java 21 is also required by the configured tooling. `gradle.properties` discovers both through `JDK21,JDK25`, with automatic JDK download disabled. Alternatively, `nix develop` supplies the configured environment on a machine with Nix. Do not change Gradle, wrapper or dependency pins to accommodate a different installed JDK.

## Build the complete port JAR

Stop dev clients and test/server JVMs that share these build outputs before compiling changed main code.

```bash
./gradlew --no-daemon --max-workers=2 :build :cad:test
```

This builds the mod and runs the configured build checks, including root tests and CAD tests. Output:

```text
build/libs/eln-26.3-0.1.0-port.jar
```

The JAR contains the native 26.3 port plus all completed ForkPatch changes in this checkout. The 1.21.1 source repository is not needed to build it. Generated resources are already committed; ordinary builds do not require datagen. Do not use a source/dev artifact in place of the normal mod JAR.

The pinned target currently is Minecraft **26.3**, NeoForge **26.3.0.75-beta**, Java **25**. Install this JAR in the `mods` directory of a matching NeoForge instance. Replace any older Electrical Age JAR so there is only one `eln` mod. Client and dedicated server should use the **same built JAR**: the build embeds a Git revision marker and rejects mismatched network versions. An uncommitted tracked change adds a dirty marker; commit completed work before producing a distributable artifact.

## Development and additional verification

```bash
# Launch a development client or dedicated server (separate runs).
./gradlew --no-daemon --max-workers=2 runClient
./gradlew --no-daemon --max-workers=2 runServer

# Full headless native GameTest suite.
./gradlew --no-daemon --max-workers=2 runGameTestServer

# Only after changing catalog/recipes/models/lang: regenerate resources.
./gradlew --no-daemon --max-workers=2 runClientData
```

Run these sequentially when they share build outputs. Use `:test` for a root-only test filter; bare `test` also reaches CAD tests.

A successful build is not evidence that GUI/gameplay checks ran. The full GameTest suite has two previously documented historical golden comparison failures (`golden_descriptors` and `golden_slots`); the final-lighting checkpoint recorded 857 individual golden assertions. Do not alter expectations or skip tests to obtain a green result. Compare fresh failures with the recorded evidence rather than assuming any failure is expected. See [final lighting validation](final-lighting-validation.json), [testing conventions](../testing.md), and [final lighting handoff](final-lighting-handoff.md).
