# Arquitetura

## Visão geral

O SafeLink 1.0.0 usa arquitetura Android nativa simples, centralizada em Activities, Services, repositório local e motor de análise.

## Pacotes principais

- `com.safelink.app`: Activities, aplicação e container.
- `com.safelink.app.data`: persistência local e modelos de terminal.
- `com.safelink.app.model`: modelos de análise e risco.
- `com.safelink.app.security`: análise local de URLs.
- `com.safelink.app.service`: serviço de proteção, acessibilidade e VPN.
- `com.safelink.app.ui`: tema e componentes reutilizáveis.

## Componentes

- `MainActivity`: dashboard, proteção, histórico, terminal e QR/câmera.
- `LinkReviewActivity`: tela de aviso e decisão.
- `HistoryDetailActivity`: detalhe de análise.
- `BrowserEntryActivity`: entrada para papel de navegador.
- `UrlAnalyzer`: heurística de risco.
- `HistoryRepository`: histórico, decisões, listas e preferências.
- `ProtectionService`: notificação de proteção visível.
- `SafeLinkAccessibilityService`: monitoramento opcional por acessibilidade.
- `SafeLinkVpnService`: VPN local DNS.

## Persistência

A persistência é local via `SharedPreferences` usando JSON.

## Fluxo principal

1. Um link chega por intent, compartilhamento, monitoramento ou terminal.
2. O `UrlAnalyzer` normaliza e classifica.
3. O resultado é salvo no histórico.
4. A tela de decisão apresenta status e ações.
5. A decisão atualiza o histórico e marca o link/domínio como resolvido para evitar reavisos automáticos imediatos.