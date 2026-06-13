# Documento de Estruturação do Projeto - SafeLink

## 1. Resumo

SafeLink é um app Android nativo para análise preventiva de links. A versão 1.0.0 entrega um fluxo local de proteção com múltiplas camadas: navegador seguro, tela de decisão, histórico, terminal, acessibilidade opcional e VPN local DNS.

## 2. Objetivo

Impedir que o usuário abra links sem antes receber uma análise clara de risco, quando o Android permite que o SafeLink participe do fluxo de abertura.

## 3. Escopo implementado

- APK/AAB full release.
- Interface em Jetpack Compose.
- Orientação fixa em retrato.
- Dashboard com métricas locais.
- Configurações de proteção.
- Análise manual de links.
- Histórico com filtros e detalhe.
- Painel terminal para links e domínios.
- Abertura de câmera pelo botão QR.
- VPN local opcional.
- AccessibilityService opcional.
- Notificação de proteção visível.

## 4. Estrutura técnica

```text
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/java/com/safelink/app/
    MainActivity.kt
    LinkReviewActivity.kt
    HistoryDetailActivity.kt
    BrowserEntryActivity.kt
    AppContainer.kt
    SafeLinkApplication.kt
    data/
    model/
    security/
    service/
    ui/
```

## 5. Dados

Persistência local via `SharedPreferences` com JSON.

## 6. Segurança

- Análise local de URL.
- Permissões sensíveis dependem de autorização do usuário.
- VPN local não exige servidor hospedado.
- Cleartext traffic desativado.
- APK release assinado.

## 7. Limitações

- O app não controla todos os navegadores internos de terceiros.
- Acessibilidade depende do conteúdo exposto pelo app de origem.
- VPN local atua por DNS/domínio.
- Não existe reputação online em tempo real.

## 8. Entrega final

- `dist/SafeLink-1.0.0-full-release.apk`.
- `dist/SafeLink-1.0.0-full-release.aab`.

Validação final: lint, testes, build e assinatura aprovados.