# UX/UI e Design System

## Identidade visual

A versão 1.0.0 usa tema escuro com fundo preto, cartões escuros, elementos brancos e acentos por status.

## Status de risco

- Seguro: verde.
- Suspeito: tom de alerta.
- Perigoso: vermelho.

## Estrutura da interface

- Dashboard inicial com anel de status, métricas do dispositivo e três cards principais.
- Botões fixos inferiores para navegação entre Proteção, Histórico, Terminal e QR.
- Cards escuros com bordas discretas.
- Histórico com barra lateral por status.
- Terminal em estilo painel de comando.
- Painel de aviso em tela preta com temporizador circular.

## Orientação

Todas as telas são fixas em retrato.

## Acessibilidade de decisão

A tela de aviso deve mostrar:

- Status.
- Score.
- URL e domínio.
- Destino final quando resolvido.
- Motivos da classificação.
- Ações claras: cancelar, abrir quando permitido e copiar.

## Limite atual

Não há biblioteca externa de design system. O design está implementado diretamente com Compose.