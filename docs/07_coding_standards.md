# Padrões de Código

## Linguagem

- Kotlin para código Android.
- XML apenas para manifesto, permissões, atalhos e configurações Android.

## UI

- Jetpack Compose.
- Componentes reutilizáveis em `ui/Components.kt` quando compartilhados.
- Tela principal concentrada em `MainActivity.kt`.

## Persistência

- Toda escrita de histórico, listas e flags deve passar por `HistoryRepository`.
- Evitar acesso direto a `SharedPreferences` fora de serviços quando houver método no repositório.

## Análise de risco

- Regras locais devem ficar em `UrlAnalyzer`.
- Motivos de classificação devem ser claros para o usuário.
- Scores devem ficar entre 0 e 100.

## Serviços

- Serviços que dependem de permissão sensível devem validar estado antes de agir.
- Acessibilidade não deve abrir aviso repetidamente para o mesmo link resolvido.

## Build

Antes de gerar release, executar:

```powershell
.\gradlew.bat --no-daemon lintFullRelease testFullDebugUnitTest assembleFullRelease bundleFullRelease
```

## Versionamento

A versão final documentada é `1.0.0` com `versionCode = 100`.