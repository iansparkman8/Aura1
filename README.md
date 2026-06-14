# Nexis Command Core

**Nexis** is a phone-first Android command center built around Ian's unmet needs:

- capture ideas before they disappear
- store code and app upgrades safely
- turn stress and chaos into one next action
- make the phone feel alive with a floating companion overlay
- keep everything local-first and review-only

This is not another generic assistant shell. Nexis starts as a living command core: flashy enough to astound people, practical enough to help you keep moving.

## What is included

- Kotlin + Jetpack Compose Android app
- animated Nexis avatar home screen
- floating overlay service using `TYPE_APPLICATION_OVERLAY`
- local memory vault backed by `SharedPreferences`
- capture screen that classifies notes as Idea, Code, Plan, Grounding, Win, or Note
- voice capture via Android speech recognition intent
- Build Lab screen for review-first code/project upgrades
- GitHub Actions workflow that builds a debug APK artifact

## Safety contract

Nexis v0.1 intentionally does **not** include:

- Accessibility Service
- screen scraping
- screenshots
- hidden remote control
- runtime code execution
- remote code loading
- hardcoded AI provider keys
- automatic repo patching

Future AI-provider connections should be user-keyed, visible, revocable, and off by default.

## Build from GitHub Actions

1. Upload this project to a GitHub repository.
2. Open the **Actions** tab.
3. Run **Build Nexis debug APK**.
4. Download the generated `nexis-debug-apk` artifact.

## Build locally

Use JDK 17 and Android SDK 35.

```bash
gradle :app:assembleDebug
```

The APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Android permissions

Nexis asks for only user-visible permissions:

- overlay permission, so the avatar can float
- notification permission, so the foreground overlay service is visible
- microphone permission, for voice capture

## Product direction

Nexis should grow in this order:

1. **Command Core** — capture, vault, grounding, build lab
2. **Living Overlay** — better movement, moods, dock behavior, reactions
3. **Project Mode** — GitHub issue/commit checklist, APK build tracker
4. **Provider Bridge** — optional user-provided AI keys, no hardcoded secrets
5. **Form Evolution** — avatar forms unlocked by saved wins, lessons, and builds
