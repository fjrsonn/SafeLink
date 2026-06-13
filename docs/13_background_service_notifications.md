# Serviços em Segundo Plano e Notificações

## ProtectionService

Mantém uma notificação de proteção ativa quando o usuário habilita proteção visível.

Função:

- Indicar que o SafeLink está ativo.
- Manter presença preventiva para o usuário.

## SafeLinkVpnService

Usa notificação própria quando a VPN local está ativa e pode notificar bloqueios de domínio.

## Permissões

- `POST_NOTIFICATIONS` em Android compatível.
- `FOREGROUND_SERVICE`.
- `FOREGROUND_SERVICE_DATA_SYNC`.
- Permissão de VPN concedida pelo usuário.

## Limites

Notificações podem ser bloqueadas ou ocultadas pelo sistema conforme configurações do usuário e fabricante.