# Modelo de Ameaças

## Ameaças consideradas

- Phishing por domínio falso.
- Links sem HTTPS.
- Encurtadores que ocultam destino.
- Punycode e homógrafos.
- Marcas sensíveis usadas em golpes.
- Links enviados por WhatsApp, SMS, email e redes sociais.
- Domínios bloqueados via DNS local.

## Ativos protegidos

- Decisão do usuário antes de abrir links.
- Histórico local de análise.
- Preferências de confiança e bloqueio.
- Integridade da experiência de navegação segura.

## Controles implementados

- Análise heurística local.
- Tela de decisão com temporizador.
- Listas locais de confiança/bloqueio.
- Monitoramento por acessibilidade opcional.
- VPN local DNS opcional.
- Persistência privada.
- Tráfego cleartext bloqueado por configuração.

## Riscos residuais

- Navegadores internos podem contornar o SafeLink.
- DNS-over-HTTPS próprio pode reduzir a efetividade da VPN.
- IP direto pode escapar de bloqueios por domínio em alguns fluxos.
- Heurísticas locais podem gerar falso positivo ou falso negativo.
- Acessibilidade depende do conteúdo exposto pelo app de origem.