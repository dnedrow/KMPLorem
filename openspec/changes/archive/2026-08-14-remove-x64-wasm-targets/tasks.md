## 1. Remove targets from build configuration

- [x] 1.1 Delete `iosX64()` declaration from `lorem/build.gradle.kts`
- [x] 1.2 Delete `linuxX64()` declaration from `lorem/build.gradle.kts`
- [x] 1.3 Delete entire `wasmJs { ... }` block from `lorem/build.gradle.kts`

## 2. Clean up build outputs and generated files

- [x] 2.1 Delete `lorem/build/bin/iosX64/` directory
- [x] 2.2 Delete `lorem/build/bin/linuxX64/` directory
- [x] 2.3 Delete `lorem/build/compileSync/wasmJs/` directory
- [x] 2.4 Delete root `build/wasm/` directory and related `kotlin-js-store/wasm/` files

## 3. Verify remaining targets

- [x] 3.1 Run `./gradlew clean` to purge stale caches
- [x] 3.2 Run `./gradlew build` and confirm jvm, android, iosArm64, and iosSimulatorArm64 targets compile and tests pass
