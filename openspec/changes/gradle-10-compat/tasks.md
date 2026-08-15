## 1. Fix Gradle 10 deprecations in build script

- [x] 1.1 Replace `androidLibrary { ... }` with `android { ... }` in `lorem/build.gradle.kts`
- [x] 1.2 Remove the no-op `withHostTestBuilder {}.configure {}` line from the android block

## 2. Suppress compile SDK advisory warning

- [x] 2.1 Add `android.suppressUnsupportedCompileSdk=37,37.0` to `gradle.properties`

## 3. Verify

- [x] 3.1 Run `./gradlew --no-configuration-cache clean build --warning-mode all` and confirm no deprecation warnings remain
