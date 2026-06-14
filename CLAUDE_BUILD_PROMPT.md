# Claude / Codespace Build Prompt for Nexis

You are working on the Android project **Nexis Command Core**.

Goal: make sure this repository builds a debug APK cleanly through GitHub Actions and local Gradle.

Hard rules:

- Use JDK 17.
- Keep Kotlin + Jetpack Compose.
- Keep minSdk 26, targetSdk 35, compileSdk 35 unless a build failure requires a minimal compatibility adjustment.
- Do not add Accessibility Service.
- Do not add screenshots or screen scraping.
- Do not add runtime code execution.
- Do not hardcode API keys.
- Do not add hidden background network calls.
- Treat Build Lab code snippets as review-only text.

Build command:

```bash
gradle :app:assembleDebug --stacktrace
```

If the build fails:

1. Read the first real Kotlin/Gradle error.
2. Patch the smallest number of files.
3. Re-run the build.
4. Keep the GitHub Actions artifact upload path:

```text
app/build/outputs/apk/debug/*.apk
```

After success, summarize:

- APK location
- files changed
- any remaining warnings
