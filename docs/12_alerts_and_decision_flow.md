# Alertas e Fluxo de Decisão

## Tela de aviso

Implementada em `LinkReviewActivity`.

A tela mostra:

- Status: Seguro, Suspeito ou Perigoso.
- Score de risco.
- URL.
- Domínio.
- Resultado de redirecionamento leve quando possível.
- Motivos da classificação.
- Temporizador de decisão.

## Ações

- Cancelar: fecha a tela sem abrir o link.
- Abrir: abre em navegador externo quando permitido.
- Copiar link: copia a URL para a área de transferência.

## Comportamento do temporizador

Se o usuário não agir no tempo definido, o fluxo cancela a abertura.

## Regras de abertura

- Links seguros podem ser abertos diretamente pela tela de decisão.
- Links suspeitos/perigosos exigem confirmação consciente.
- Em modo rígido, links perigosos podem ter abertura bloqueada.

## Pós-decisão

A decisão é registrada no histórico e o link/domínio é marcado como resolvido para evitar reaviso automático imediato.