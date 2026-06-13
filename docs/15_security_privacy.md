# Segurança e Privacidade

## Dados locais

A versão 1.0.0 não possui backend remoto. Histórico, listas e preferências ficam no armazenamento privado do app.

## Permissões sensíveis

### Acessibilidade

Usada somente com ativação explícita do usuário para detectar links acionados em outros apps. É uma permissão sensível e deve ser explicada ao usuário.

### VPN

Usada para bloqueio DNS local. Não exige hospedagem externa.

## Rede

- Cleartext traffic desativado.
- A resolução de redirecionamento usa requisição leve para verificar destino final quando possível.

## Keystore

O APK release é assinado com `dist/safelink-release.keystore`.

## Limitações de segurança

- A análise é heurística local.
- Não há verificação online de reputação.
- Não há proteção absoluta contra navegador interno de terceiros.
- Não há inspeção de tráfego HTTPS completo.

## Privacidade operacional

Não documentar nem registrar dados sensíveis além de URLs/domínios necessários para histórico e decisões locais.