# Contratos de API

## Estado atual

A versão 1.0.0 não possui API remota, backend ou contrato HTTP próprio.

## Contratos internos reais

### Entrada de análise

- Texto com URL ou domínio.

### Saída de análise

- `AnalysisResult` com URL normalizada, host, nível, score e motivos.

### Decisão local

- `Aberto`.
- `Cancelado`.
- `Copiado`.
- `Confiado`.
- `Bloqueado`.

### Listas locais

- Domínios confiáveis.
- Domínios bloqueados.
- Decisões temporárias.

## Futuro

Qualquer API remota deve ser documentada somente quando existir implementação real. No estado atual, não há endpoints para listar.