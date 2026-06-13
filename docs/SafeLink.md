# Fundamentação e Contextualização do SafeLink

SafeLink é um aplicativo Android criado para reduzir riscos no momento em que o usuário interage com links externos. O foco é apoiar pessoas mais vulneráveis a golpes digitais, como idosos, crianças e usuários com pouca familiaridade com segurança da informação.

## Problema

Golpes digitais frequentemente usam links em mensagens, redes sociais, emails e SMS para induzir o usuário a abrir páginas falsas. Muitos ataques exploram urgência, promessas de recompensa, marcas conhecidas, bancos, governo, Pix, Correios e redes sociais.

## Solução proposta

O SafeLink atua como uma camada preventiva. Quando possível, ele recebe o link antes da abertura, analisa localmente o risco e mostra uma tela de decisão com informações simples.

## Camadas de proteção

- Navegador seguro para links HTTP/HTTPS.
- Compartilhamento de links para análise.
- Monitoramento opcional por acessibilidade.
- VPN local opcional para bloqueio DNS.
- Listas locais de confiança e bloqueio.
- Histórico e detalhe de decisões.

## Valor social

O app busca promover inclusão digital e segurança preventiva, ajudando o usuário a entender por que um link pode ser perigoso antes de seguir para o destino.

## Estado atual

A versão 1.0.0 está pronta como APK/AAB full release, com interface escura, orientação fixa em retrato e funcionamento local sem backend próprio.

## Limites importantes

O SafeLink não garante bloqueio absoluto de todos os links em todos os apps. O Android, navegadores internos e permissões do usuário determinam quando o app consegue agir antes da abertura.