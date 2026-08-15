## MODIFIED Requirements

### Requirement: Multiplatform target coverage

The library SHALL publish artifacts for the JVM, Android, `iosArm64`, and `iosSimulatorArm64` targets. All behavior SHALL be implemented in the common source set with no platform-specific declarations, so that every target exhibits identical behavior. The library SHALL declare no runtime dependencies beyond the Kotlin standard library. The public API SHALL be explicit, with every public declaration carrying an explicit visibility modifier and return type.

#### Scenario: Every target builds and passes the same tests

- **WHEN** the common test suite is run against each supported target
- **THEN** every target compiles and every test passes

#### Scenario: No platform-specific declarations exist

- **WHEN** the source tree is inspected
- **THEN** no `expect` or `actual` declaration is present and all library code resides in the common source set

#### Scenario: No runtime dependencies are declared

- **WHEN** a consumer resolves the published artifact
- **THEN** the only transitive runtime dependency is the Kotlin standard library

## REMOVED Requirements

### Requirement: iosX64 target support
**Reason**: The iosX64 target (Intel Mac simulator) is deprecated by Apple; all modern development uses iosSimulatorArm64.
**Migration**: Consumers on Intel Macs should use Rosetta 2 with the iosSimulatorArm64 artifact.

### Requirement: linuxX64 target support
**Reason**: No known consumers; adds CI complexity without delivering value.
**Migration**: Build from source if linuxX64 is needed.

### Requirement: wasmJs target support
**Reason**: Requires browser/Node WASM toolchains that complicate the build; no active consumers.
**Migration**: Consumers needing WASM should build from source or use the JVM target via a backend.
