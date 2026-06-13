# Guia Técnico Android

## Abrir no Android Studio

1. Abrir a pasta raiz `SafeLink`.
2. Aguardar sincronização Gradle.
3. Selecionar flavor `full`.
4. Rodar em emulador ou dispositivo físico.

## Gerar release

```powershell
$env:JAVA_HOME = Join-Path $PWD '.tools\jdk17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon lintFullRelease testFullDebugUnitTest assembleFullRelease bundleFullRelease
```

## Instalar no dispositivo via ADB

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "dist\SafeLink-1.0.0-full-release.apk"
```

## Configurar camadas

- Abrir SafeLink.
- Ativar proteção visível.
- Definir como navegador seguro/default browser.
- Ativar monitoramento em Acessibilidade, se desejar.
- Ativar VPN local, se desejar.

## Orientação

O app está travado em retrato pelo manifesto.

## Diagnóstico

Usar Logcat do Android Studio para depurar Activities, AccessibilityService e VpnService.