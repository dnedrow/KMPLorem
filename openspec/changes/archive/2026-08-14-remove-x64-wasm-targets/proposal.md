## Why

The X64 targets (`iosX64`, `linuxX64`) and `wasmJs` add CI/build complexity without delivering value to the project's current audience. `iosX64` is the Intel-Mac simulator (deprecated by Apple), `linuxX64` has no known consumers, and `wasmJs` requires browser/Node toolchains that complicate the build. Removing them simplifies maintenance and speeds up builds.

## What Changes

- **BREAKING** Remove `iosX64()` target declaration and associated source sets/build outputs.
- **BREAKING** Remove `linuxX64()` target declaration and associated source sets/build outputs.
- **BREAKING** Remove `wasmJs { ... }` target block and associated source sets/build outputs.
- Clean up any CI, Gradle, or documentation references to these targets.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `lorem-generator`: Platform target matrix is narrowing — the supported-platforms requirement changes.

## Impact

- `lorem/build.gradle.kts`: Target declarations removed.
- Published Maven coordinates will no longer include `iosX64`, `linuxX64`, or `wasmJs` variants.
- `build/` artifacts for removed targets can be deleted.
- Any CI matrix entries for these targets should be removed.
- Consumers currently depending on these platform artifacts will need to migrate or build from source.
