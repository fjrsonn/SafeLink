# Modelo de Dados Local

## Banco de dados

Não há banco de dados relacional na versão 1.0.0. O app usa `SharedPreferences` com JSON.

## Entidade AnalysisResult

Campos:

- `url`.
- `host`.
- `level`.
- `reasons`.
- `score`.
- `decision`.
- `checkedAt`.
- `occurrences`.
- `eventTimes`.

## Entidade TerminalEntry

Campos:

- `type`.
- `value`.
- `status`.
- `host`.
- `score`.
- `reasons`.
- `checkedAt`.

## Chaves locais principais

- `history`.
- `terminal_entries`.
- `trusted_domains`.
- `blocked_domains`.
- `blocked_events`.
- `layer_protection_visible`.
- `layer_browser`.
- `layer_accessibility`.
- `layer_vpn`.
- `block_shorteners`.
- `block_no_https`.
- `block_ip_domains`.

## Observação

Se o projeto crescer, pode ser considerada migração para banco local. Isso não está implementado na versão atual.