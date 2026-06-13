# Composer - Especificação Atual do SafeLink

## Identificação

Nome: SafeLink  
Tipo: Aplicativo Android nativo  
Versão: 1.0.0  
Formato de entrega: APK e AAB full release

## Objetivo

Criar uma camada preventiva de decisão antes que o usuário abra links potencialmente perigosos no Android.

## Stack real

- Kotlin.
- Jetpack Compose.
- Material 3.
- AndroidX.
- SharedPreferences com JSON.
- Foreground Service.
- AccessibilityService.
- VpnService.
- JUnit/Robolectric.

## Estrutura real

```text
app/src/main/java/com/safelink/app/
  MainActivity.kt
  LinkReviewActivity.kt
  HistoryDetailActivity.kt
  BrowserEntryActivity.kt
  AppContainer.kt
  SafeLinkApplication.kt
  data/HistoryRepository.kt
  model/AnalysisResult.kt
  model/RiskLevel.kt
  security/UrlAnalyzer.kt
  service/ProtectionService.kt
  service/SafeLinkAccessibilityService.kt
  service/SafeLinkVpnService.kt
  ui/Components.kt
  ui/SafeLinkTheme.kt
```

## Funcionalidades implementadas

- Dashboard inicial.
- Navegação inferior: Proteção, Histórico, Terminal e QR.
- Análise local de URL.
- Tela de aviso com decisão.
- Histórico e detalhe.
- Terminal para inserir links/domínios.
- Listas locais de confiança e bloqueio.
- VPN local DNS.
- Monitoramento por acessibilidade.
- Proteção visível por notificação.
- Atalho de análise.
- Orientação fixa em retrato.

## Build final

```powershell
.\gradlew.bat --no-daemon lintFullRelease testFullDebugUnitTest assembleFullRelease bundleFullRelease
```

Artefatos:

- `dist/SafeLink-1.0.0-full-release.apk`.
- `dist/SafeLink-1.0.0-full-release.aab`.