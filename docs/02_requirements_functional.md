# Requisitos Funcionais

## Implementados na versão 1.0.0

- Analisar links digitados, compartilhados ou abertos pelo Android quando enviados ao SafeLink.
- Classificar links como Seguro, Suspeito ou Perigoso.
- Exibir tela de decisão com status, score, motivos e ações.
- Permitir configuração como navegador seguro/default browser.
- Monitorar links acionados pelo usuário com AccessibilityService, quando autorizado.
- Ativar VPN local opcional para bloqueio DNS.
- Registrar histórico de análises e decisões.
- Exibir detalhe completo de item do histórico.
- Confiar ou bloquear domínios localmente.
- Inserir links e domínios pelo painel terminal.
- Abrir câmera pelo botão QR.
- Manter o app somente na vertical.

## Ações da tela de decisão

- Cancelar.
- Abrir quando permitido.
- Copiar link.

## Limites funcionais

- Acessibilidade depende do conteúdo exposto por outros apps.
- VPN local atua por domínio/DNS, não pela URL completa em HTTPS.
- O Android pode permitir que alguns apps usem navegador interno sem acionar o SafeLink.