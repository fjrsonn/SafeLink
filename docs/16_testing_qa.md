# Testes e QA

## Testes automatizados

Arquivo atual:

- `app/src/test/java/com/safelink/app/security/UrlAnalyzerTest.kt`

Quantidade validada na versão 1.0.0:

- 6 testes.
- 0 falhas.

Cenários cobertos:

- Link de phishing bancário.
- Lookalike de Instagram.
- Encurtador suspeito.
- Ameaça em email.
- Golpe por SMS/Correios.
- Link seguro da documentação Android.

## Validação de release

Comando executado:

```powershell
.\gradlew.bat --no-daemon lintFullRelease testFullDebugUnitTest assembleFullRelease bundleFullRelease
```

Resultado: build concluído com sucesso.

## Verificação de assinatura

APK final verificado com `apksigner` e assinatura v2 válida.

## Testes manuais recomendados

- Instalar APK em dispositivo físico.
- Definir SafeLink como navegador padrão.
- Ativar proteção visível.
- Ativar monitoramento de acessibilidade.
- Ativar VPN local.
- Abrir links seguros, suspeitos e perigosos por WhatsApp/SMS/email.
- Verificar histórico e detalhes.
- Inserir domínios no terminal.
- Confirmar que o app permanece em retrato ao girar o celular.