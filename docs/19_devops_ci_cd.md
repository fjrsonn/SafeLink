# DevOps e CI/CD

## Estado atual

Não há pipeline CI/CD configurado no repositório. O build final foi executado localmente via Gradle.

## Build local final

```powershell
$env:JAVA_HOME = Join-Path $PWD '.tools\jdk17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon lintFullRelease testFullDebugUnitTest assembleFullRelease bundleFullRelease
```

## Saídas

- APK release em `app/build/outputs/apk/full/release/`.
- AAB release em `app/build/outputs/bundle/fullRelease/`.
- Cópias finais em `dist/`.

## Recomendação futura

Caso o projeto avance para publicação recorrente, criar pipeline para:

- Lint.
- Testes unitários.
- Build full release.
- Verificação de assinatura.
- Publicação controlada do AAB.

Nenhum desses itens está automatizado no estado atual.