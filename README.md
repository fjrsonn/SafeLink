# SafeLink

SafeLink is a native Android security app built with Kotlin and Jetpack Compose. It helps users inspect suspicious links before opening them, combining local URL analysis, Android intent interception, optional accessibility monitoring, and an optional local VPN DNS guard.

The project is designed for preventive protection: when Android allows SafeLink to receive a link, the app shows a review screen with the detected risk level, the reasons behind the classification, and clear actions to cancel, copy, trust, block, or continue.

## Highlights

- Native Android app written in Kotlin.
- Jetpack Compose interface with Material 3 components.
- Local URL risk analysis without a required backend.
- HTTP/HTTPS intent handling for browser/default-app flows.
- Share target support for links sent from other apps.
- Manual analyzer for pasted links and domains.
- Local history with decisions, risk scores, occurrences, and details.
- Trusted and blocked domain lists.
- Optional `AccessibilityService` monitoring for visible link text.
- Optional `VpnService` DNS layer for local domain blocking.
- `full` and `lite` product flavors.
- Unit tests for the URL analyzer.

## Current Scope

SafeLink focuses on reducing link-opening risk on Android devices. It does not replace Android's security model, browser sandboxing, antivirus software, or user judgment.

Implemented in the current app:

- SafeLink dashboard.
- Link review activity.
- Secure browser entry activity.
- Local URL scoring engine.
- Foreground protection service.
- Accessibility-based link monitor.
- Local VPN DNS guard.
- History and detail screens.
- Backup/export helpers for user-managed lists.
- Documentation under `docs/`.

Out of scope for this version:

- Cloud synchronization.
- User accounts.
- Remote reputation API.
- Web dashboard.
- Play Store publishing pipeline.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- AndroidX Core KTX
- Lifecycle Runtime
- Navigation Compose dependency
- SharedPreferences and JSON persistence
- JUnit 4
- Robolectric
- Java 17

## Project Structure

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   ├── java/com/safelink/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── LinkReviewActivity.kt
│       │   │   ├── BrowserEntryActivity.kt
│       │   │   ├── data/
│       │   │   ├── model/
│       │   │   ├── security/
│       │   │   ├── service/
│       │   │   └── ui/
│       │   └── res/
│       ├── lite/
│       └── test/
├── docs/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Requirements

- Android Studio or compatible Android SDK installation.
- JDK 17.
- Gradle Wrapper included in this repository.
- Android SDK platform 35.

## Getting Started

Clone the repository:

```bash
git clone https://github.com/fjrsonn/SafeLink.git
cd SafeLink
```

Run unit tests:

```bash
./gradlew testFullDebugUnitTest
```

Build debug APK:

```bash
./gradlew assembleFullDebug
```

Build lite debug APK:

```bash
./gradlew assembleLiteDebug
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat testFullDebugUnitTest
.\gradlew.bat assembleFullDebug
```

## Release Signing

Release signing credentials are intentionally not committed. Configure them locally using environment variables or `local.properties`.

Environment variables:

```bash
SAFELINK_RELEASE_STORE_FILE=path/to/safelink-release.keystore
SAFELINK_RELEASE_STORE_PASSWORD=your-store-password
SAFELINK_RELEASE_KEY_ALIAS=your-key-alias
SAFELINK_RELEASE_KEY_PASSWORD=your-key-password
```

Equivalent `local.properties` entries:

```properties
safelink.release.storeFile=dist/safelink-release.keystore
safelink.release.storePassword=your-store-password
safelink.release.keyAlias=your-key-alias
safelink.release.keyPassword=your-key-password
```

Build release artifacts after configuring signing:

```bash
./gradlew assembleFullRelease
./gradlew bundleFullRelease
```

Generated APKs, AABs, keystores, and local build outputs are ignored by Git.

## How SafeLink Analyzes Links

The local analyzer scores several risk signals:

- Missing HTTPS.
- Known URL shorteners.
- Punycode and internationalized domain risks.
- Mixed-script domains.
- Long or unusual host names.
- Sensitive terms commonly used in phishing.
- Brand-like or lookalike host patterns.
- Direct IP-address usage.

The result is mapped to a risk level:

- Safe
- Suspicious
- Dangerous

Each result includes a score and human-readable reasons so the user can understand the decision before continuing.

## Android Protection Layers

SafeLink uses multiple Android mechanisms because no single interception method covers every app:

- Browser/default-app entry: receives HTTP/HTTPS links when Android routes them to SafeLink.
- Share target: reviews text links shared from other apps.
- Accessibility service: optionally observes selected UI text and opens review when a URL is detected.
- Local VPN service: optionally handles DNS-level domain blocking using local policy.

Some apps use internal browsers, private renderers, custom DNS, cache, or flows that limit interception. SafeLink documents these limits instead of claiming complete coverage.

## Documentation

The `docs/` directory contains deeper technical documentation, including:

- Product vision.
- Functional and non-functional requirements.
- Architecture.
- Android foundation.
- Interception engine.
- URL analysis engine.
- Security and privacy notes.
- Testing and QA.
- Release and distribution notes.
- Future roadmap.

Start with:

- `docs/00_README.md`
- `docs/05_architecture.md`
- `docs/10_interception_engine.md`
- `docs/11_url_analysis_engine.md`
- `docs/16_testing_qa.md`

## Testing

Primary test command:

```bash
./gradlew testFullDebugUnitTest
```

The current unit tests cover representative URL-analysis cases:

- Phishing-style bank/login links.
- Brand-like suspicious links.
- Shortened links.
- HTTP links with sensitive wording.
- Safe documentation links.

## Security Notes

- Do not commit release keystores.
- Do not commit signing passwords.
- Do not commit `local.properties`.
- Treat generated APK/AAB files as release artifacts, not source code.
- Review permissions and Android service behavior before distribution.

## License

No license file is currently included. Add a license before distributing or accepting external contributions.
