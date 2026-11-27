# Qwen ports and remaining controls

Integration started at 232ea831615f. Candidate range b34d43a6e..c8a98528d. Qwen full build passed, and 2103 of 2106 GameTests passed with the three existing failure IDs. The sag baseline finished at 2125 of 2128 with the same failures.

Applied the candidate's source, assets and tests with Git's three-way patch application. It merged cleanly onto the newer recipes and manifest. Generated resources will be regenerated on this branch. Original source comparison confirms drill time/energy and all four heater voltage/power pairs and icon keys.

Next: generate resources, verify the six focused test classes, commit the six ports with candidate attribution, then implement the four controls and multicolor meter.

Both integration datagen runs passed. The second wrote zero files. All 36 candidate tests passed on the integration branch.

The six-port checkpoint is 8031d88ea and was pushed. The four wireless controls use IDs 6100–6103, source plate models and recipes, and the native wireless channel, menu, animation and placement code. Wall switches retain state. Wall buttons restart their pulse when pressed and reset on load as in ForkPatch. The LED meter uses ID 6537, the source icon and plank-tag recipe, and the native LED renderer with the source hue and off threshold.

All eight final focused GameTests passed, including the original switch/button controls, new wall mounts, native receiver output, emitted meter colors and the harness persistence checks. Final datagen wrote zero files on repeat. All eleven added mappings preserve every previous registry mapping. No live client session was used.

Full validation commands: ./gradlew build and ./gradlew runGameTestServer, with logs /tmp/eln-eleven-build.log and /tmp/eln-eleven-game.log. Fresh artifact and checksum will be in build/libs after clean assembly.
