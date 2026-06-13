# Stack Técnica

## Android

- Kotlin.
- Jetpack Compose.
- Material 3.
- Activity Compose.
- AndroidX Core KTX.
- Lifecycle Runtime.
- Navigation Compose está disponível como dependência, mas a navegação principal atual é controlada em Compose dentro da `MainActivity`.

## Serviços Android

- Foreground Service para proteção visível.
- AccessibilityService para monitoramento opcional.
- VpnService para bloqueio DNS local.

## Persistência

- SharedPreferences.
- JSON com `org.json`.

## Testes

- JUnit 4.
- Robolectric.
- kotlinx-coroutines-test.

## Build

- Gradle Android Plugin.
- Kotlin Compose Plugin.
- Java 17.
- Flavors: `full` e `lite`.
- Release assinado com keystore local em `dist/safelink-release.keystore`.