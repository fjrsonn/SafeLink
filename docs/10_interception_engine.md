# Motor de Interceptação

## Camadas implementadas

### Navegador seguro

O app registra filtros HTTP/HTTPS para receber links quando configurado como navegador padrão ou opção de abertura.

Arquivos principais:

- `BrowserEntryActivity.kt`
- `LinkReviewActivity.kt`
- `AndroidManifest.xml`

### Compartilhamento

`LinkReviewActivity` aceita `ACTION_SEND` com `text/plain` e extrai a primeira URL encontrada.

### AccessibilityService

`SafeLinkAccessibilityService` monitora eventos de clique e clique longo quando ativado pelo usuário. Ele procura URLs no texto do elemento acionado.

Limite: não garante leitura de todos os apps ou navegadores internos.

### VPN local

`SafeLinkVpnService` cria uma VPN local para responder/bloquear consultas DNS com base em domínios confiáveis, bloqueados e análise local.

Limite: atua por domínio, não por URL completa.

## Prevenção de repetição

Links e domínios resolvidos por uma ação do usuário são marcados temporariamente para evitar reabertura automática do aviso pelo monitoramento de acessibilidade.