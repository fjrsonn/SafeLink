# Configurações, Persistência e Histórico

## Persistência real

A versão 1.0.0 usa `SharedPreferences` privado com JSON.

Classe central:

- `HistoryRepository`.

## Dados persistidos

- Histórico de análises.
- Decisões por link.
- Ocorrências e horários de eventos.
- Domínios confiáveis.
- Domínios bloqueados.
- Domínios temporariamente confiados/bloqueados.
- Flags das camadas de proteção.
- Entradas do terminal.
- Eventos de bloqueio VPN.

## Histórico

Cada URL é normalizada para evitar duplicação simples. Quando uma URL aparece novamente, o app soma ocorrências e registra novo horário de evento.

## Terminal

O painel terminal registra apenas entradas inseridas pelo usuário no próprio painel.

## Exclusão

O usuário pode remover itens do histórico e remover decisões de domínio.