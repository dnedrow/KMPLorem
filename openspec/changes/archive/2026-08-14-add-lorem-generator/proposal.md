## Why

DSKit's Swift `DSLorem` module gives Apple-platform teams deterministic, seeded Lorem Ipsum for previews, snapshot tests, and documentation — but it stops at the Apple boundary. Android, desktop, and web surfaces that need the same placeholder copy either hand-roll their own generator or paste static strings, which drift from the Swift output and cannot be pinned to a seed.

This change ports `DSLorem` to a standalone Kotlin Multiplatform library so every platform draws placeholder prose from one specified, reproducible generator.

## What Changes

- **New KMP library project** built on the JetBrains multiplatform-library-template layout: a root Gradle build, a `:lorem` module, a version catalog, and the wrapper.
- **Seven targets** — `jvm`, `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, `iosX64`, `linuxX64`, `wasmJs` — all served by a single `commonMain` source set with no `expect`/`actual` declarations.
- **A deterministic generator** matching the behavior specified in DSKit's `dslorem-generator` spec: SplitMix64-seeded randomness, weighted sentence templates over six grammatical patterns, `{wN}` token rendering, five paragraph styles, Latin transition phrases, anti-repetition, configurable word bounds, and the opt-in canonical `Lorem ipsum dolor sit amet` opening.
- **An embedded 210-word Latin dictionary** compiled into `commonMain` rather than loaded from a resource bundle, keeping the library free of platform file access.
- **Typed failures** through a sealed `LoremException` hierarchy rooted in `IllegalArgumentException`, annotated `@Throws` so Kotlin/Native callers receive catchable errors instead of crashes.
- **Explicit API mode** enabled, so every public declaration carries an explicit visibility and return type.
- **Behavioral parity, not byte parity.** The Kotlin library satisfies the same requirements as the Swift module but is not required to emit identical text for a given seed. Golden tests are authored fresh against the Kotlin implementation.
- Publishing coordinates are left as placeholders; the release destination is decided before the first release and is out of scope here.

## Capabilities

### New Capabilities

- `lorem-generator`: Deterministic, seeded Lorem Ipsum generation for Kotlin Multiplatform — turning a seed, a word dictionary, and weighted sentence templates into reproducible paragraphs with configurable styles, optional transitions, an opt-in canonical opening, and anti-repetition, without shared mutable state.

### Modified Capabilities

None. This project has no existing specs.

## Impact

- **New repository content.** The project is currently an empty OpenSpec workspace; this change introduces the entire Gradle build and source tree.
- **New public API surface.** `LoremGenerator`, `LoremDictionary`, `GeneratorConfiguration`, `ParagraphStyle`, `SentenceTemplate` (and its `Pattern`), `TemplateLibrary`, `TransitionLibrary`, `TemplateRenderer`, `PhraseGenerator`, `WeightedSelector`, `SeededRandom`, and `LoremException` under `io.github.dnedrow.lorem`.
- **Dependencies.** None at runtime. `kotlin-test` in `commonTest`; the Android Gradle plugin, Kotlin Multiplatform plugin, and Vanniktech publishing plugin at build time.
- **CI.** The template's build matrix enumerates task names by hand; it gains entries for the added targets.
- **Relationship to DSKit.** No code or build coupling. The Swift module is the behavioral reference, and the two evolve independently after this change.
