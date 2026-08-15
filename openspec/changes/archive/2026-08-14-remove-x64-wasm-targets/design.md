## Context

The project currently declares six Kotlin Multiplatform targets: `jvm`, `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, `iosX64`, `linuxX64`, and `wasmJs`. See proposal.md for motivation behind removing the last three.

The `wasmJs` block also pulls in Node/browser toolchain configuration and disables browser tests. All library code lives in `commonMain` with no `expect`/`actual` declarations, so removing targets is purely a build configuration change.

## Goals / Non-Goals

**Goals:**
- Remove `iosX64()`, `linuxX64()`, and `wasmJs { ... }` from the Kotlin targets block
- Remove any build outputs, CI references, or documentation mentioning these targets
- Keep the remaining targets (jvm, android, iosArm64, iosSimulatorArm64) fully functional

**Non-Goals:**
- Adding new targets to replace those removed
- Migrating existing consumers — this is a breaking change communicated via release notes
- Changing any library source code (all code is in commonMain)

## Decisions

### Remove targets in `lorem/build.gradle.kts`

**Choice**: Delete the `iosX64()`, `linuxX64()`, and entire `wasmJs { ... }` block declarations.

**Rationale**: All code is common — no platform source sets reference these targets. Removal is a one-line-per-target edit.

**Alternative considered**: Keep targets but disable tests — rejected because it still requires maintaining toolchain compatibility and publishing empty-value artifacts.

### Clean up build outputs

**Choice**: Delete residual `build/` directories for removed targets (e.g., `build/bin/iosX64`, `build/bin/linuxX64`, `build/compileSync/wasmJs`, `build/wasm/`).

**Rationale**: Stale build artifacts confuse tooling and waste disk space. They will not regenerate after target removal.

## Risks / Trade-offs

- **[Breaking change for iosX64 consumers]** → Mitigated: Apple has deprecated Intel simulators; Rosetta 2 runs arm64 simulators on Intel Macs.
- **[Breaking change for wasmJs consumers]** → Mitigated: No known consumers; documented in release notes.
- **[Stale Gradle caches]** → Run `./gradlew clean` after applying changes to avoid phantom references.
