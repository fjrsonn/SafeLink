# Fundação Android

## Configuração

- `applicationId`: `com.safelink.app`.
- `minSdk`: 29.
- `targetSdk`: 35.
- `compileSdk`: 35.
- Versão: `1.0.0`.

## Activities

- `MainActivity`: tela principal.
- `LinkReviewActivity`: revisão de link.
- `HistoryDetailActivity`: detalhe do histórico.
- `BrowserEntryActivity`: entrada de navegador.

Todas estão travadas em `portrait`.

## Permissões

- `INTERNET`.
- `POST_NOTIFICATIONS`.
- `FOREGROUND_SERVICE`.
- `FOREGROUND_SERVICE_DATA_SYNC`.
- `BIND_ACCESSIBILITY_SERVICE` no serviço de acessibilidade.
- `BIND_VPN_SERVICE` no serviço VPN.

## Intents

- HTTP/HTTPS para links externos.
- SEND `text/plain` para compartilhamento.
- APP_BROWSER para atuar como navegador.
- Captura de imagem para QR/câmera.

## Segurança de rede

`cleartextTrafficPermitted="false"` no `network_security_config`.