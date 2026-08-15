## Why

The build emits Gradle deprecation warnings that will become hard errors in Gradle 10:
1. "Using a Project object as a dependency notation" — from AGP's `KotlinMultiplatformAndroidPlugin.createUnitTestComponent`, triggered by the redundant `withHostTestBuilder {}.configure {}` call.
2. "'androidLibrary' block is deprecated. Please use 'android' instead" — the KMP Android target DSL has been renamed.

Fixing these now keeps the build clean on Gradle 9.6 and forward-compatible with Gradle 10.

## What Changes

- Replace `androidLibrary { ... }` with `android { ... }` in `lorem/build.gradle.kts`.
- Remove the no-op `withHostTestBuilder {}.configure {}` call from the android block.
- Add `android.suppressUnsupportedCompileSdk=37,37.0` to `gradle.properties` to suppress the advisory SDK version warning.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

_(none — this is a pure build-tooling fix with no behavior change; `skip_specs: true` is set)_

## Impact

- `lorem/build.gradle.kts`: One line removed.
- `gradle.properties`: One property added.
- No behavioral change to library output or published artifacts.
