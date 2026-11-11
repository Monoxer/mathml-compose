# Contributing

## Review expectations

We appreciate external contributions. However, at the current stage, we cannot guarantee that time
or effort will be available to review them. Pull requests may remain unreviewed or be closed without
review.

In particular, we do not have the resources to review AI-generated code. Pull requests with
AI-generated code may be closed without review.

## Testing

```bash
./gradlew jvmTest                    # JVM
./gradlew testDebugUnitTest          # Android
./gradlew wasmJsTest                 # Wasm
./gradlew iosSimulatorArm64Test      # iOS simulator; requires macOS
./gradlew build                      # build and test all available targets
```

## Formatting and static analysis

```bash
./gradlew ktlintCheck          # check Kotlin formatting and style
./gradlew ktlintFormat         # format Kotlin sources
./gradlew detekt               # run static analysis and auto-correct supported findings
```
