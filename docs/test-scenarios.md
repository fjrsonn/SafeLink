# SafeLink - Cenários de Teste

## Objetivo

Validar se a versão 1.0.0 classifica links, exibe aviso quando intercepta o fluxo e mantém histórico coerente.

## Preparação

1. Instalar `dist/SafeLink-1.0.0-full-release.apk`.
2. Abrir SafeLink.
3. Ativar proteção visível.
4. Definir SafeLink como navegador seguro/default browser.
5. Ativar monitoramento de acessibilidade, se o teste exigir.
6. Ativar VPN local, se o teste exigir.
7. Confirmar que a tela não gira ao mudar a orientação do aparelho.

## Links de referência

| Caso | Link | Resultado esperado |
| --- | --- | --- |
| Seguro | `https://developer.android.com/privacy-and-security` | Seguro |
| Suspeito | `https://bit.ly/presente-pix` | Suspeito |
| Perigoso | `http://login-banco-premio-verificar-senha.com` | Perigoso |
| Email falso | `http://seguranca-paypal-confirmar.example.com/login` | Perigoso |
| SMS falso | `http://correios-taxas-urgente.example.com` | Perigoso |

## Canais de teste

| Canal | Esperado |
| --- | --- |
| WhatsApp | SafeLink intercepta se for chamado como navegador/default browser ou se acessibilidade detectar o clique. |
| SMS | Deve chamar navegador padrão em muitos aparelhos. |
| Email | Deve chamar navegador padrão em muitos clientes. |
| Instagram/TikTok | Pode usar navegador interno; testar compartilhar/copiar para SafeLink. |
| Navegador externo | SafeLink abre primeiro se configurado como padrão. |
| Terminal SafeLink | Inserção manual sempre deve analisar ou registrar domínio. |

## VPN local

Com VPN ativa, tentar carregar domínio perigoso e verificar se o carregamento é bloqueado por DNS quando aplicável.

## Histórico

Após cada ação:

- Confirmar registro no histórico.
- Confirmar contagem de ocorrência.
- Confirmar detalhe do item.
- Confirmar que ações tomadas não ficam reaparecendo como novo aviso automático em sequência.

## Resultado da suíte automatizada

- 6 testes unitários em `UrlAnalyzerTest`.
- Todos aprovados na build final.