# SafeLink

SafeLink é um aplicativo Android nativo de segurança preventiva contra links suspeitos, phishing e golpes digitais. Ele ajuda o usuário a revisar um destino antes de abrir, combinando análise local de URL, interceptação por intents do Android, monitoramento opcional por acessibilidade e uma camada opcional de VPN local para bloqueio DNS.

O objetivo do projeto é colocar uma etapa clara de decisão entre o clique e o destino final sempre que o Android permitir a interceptação. A tela de revisão mostra o nível de risco, os motivos da classificação e ações diretas para cancelar, copiar, confiar, bloquear ou continuar.

## Destaques

- Aplicativo Android nativo escrito em Kotlin.
- Interface em Jetpack Compose com Material 3.
- Análise local de URLs sem dependência obrigatória de backend.
- Interceptação de links HTTP/HTTPS por fluxo de navegador ou app padrão.
- Suporte a compartilhamento de links recebidos de outros aplicativos.
- Analisador manual para links e domínios colados pelo usuário.
- Histórico local com decisões, pontuação de risco, ocorrências e detalhes.
- Listas locais de domínios confiáveis e bloqueados.
- Monitoramento opcional via `AccessibilityService`.
- Bloqueio DNS opcional via `VpnService` local.
- Flavors `full` e `lite`.
- Testes unitários para o motor de análise de URL.

## Apresentação da Interface

Use esta área para inserir imagens reais do aplicativo. Substitua os caminhos abaixo pelos arquivos de screenshot que você adicionar ao repositório, por exemplo `docs/images/01-dashboard.png`.

### 1. Dashboard Principal

![Dashboard principal](docs/images/01-dashboard-principal.png)

Texto modelo: A tela inicial apresenta o estado geral da proteção do SafeLink, destacando as camadas ativas, os indicadores de segurança e o acesso rápido às funções principais do aplicativo.

### 2. Cartão Central de Proteção

![Cartão central de proteção](docs/images/02-cartao-protecao.png)

Texto modelo: Esta área concentra o status de proteção em tempo real, permitindo que o usuário entenda rapidamente se o aplicativo está pronto para analisar links e reduzir riscos durante a navegação.

### 3. Analisador Manual de Links

![Analisador manual](docs/images/03-analisador-manual.png)

Texto modelo: O analisador manual permite colar links ou domínios para avaliação imediata, exibindo uma classificação local antes que o usuário decida abrir ou bloquear o destino.

### 4. Tela de Revisão de Link

![Revisão de link](docs/images/04-revisao-link.png)

Texto modelo: A tela de revisão exibe o endereço analisado, o domínio identificado, o nível de risco e as ações disponíveis para continuar, copiar, cancelar ou aplicar uma decisão de confiança.

### 5. Alerta de Link Perigoso

![Alerta de link perigoso](docs/images/05-alerta-perigoso.png)

Texto modelo: Quando um link apresenta sinais fortes de golpe ou phishing, o SafeLink destaca o risco com uma interface de alerta clara, explicando os motivos antes de qualquer abertura externa.

### 6. Motivos da Classificação

![Motivos da classificação](docs/images/06-motivos-classificacao.png)

Texto modelo: Cada análise inclui justificativas legíveis, como ausência de HTTPS, uso de encurtadores, termos suspeitos ou padrões de domínio que podem indicar tentativa de fraude.

### 7. Histórico de Análises

![Histórico de análises](docs/images/07-historico-analises.png)

Texto modelo: O histórico organiza os links verificados, suas pontuações e decisões anteriores, facilitando a revisão de eventos e a identificação de padrões recorrentes.

### 8. Detalhes do Histórico

![Detalhes do histórico](docs/images/08-detalhes-historico.png)

Texto modelo: A tela de detalhes aprofunda uma análise específica, mostrando informações do domínio, pontuação, motivos detectados e contexto da decisão registrada.

### 9. Listas de Confiança e Bloqueio

![Listas de confiança e bloqueio](docs/images/09-listas-dominios.png)

Texto modelo: As listas locais permitem gerenciar domínios confiáveis e bloqueados, dando ao usuário controle direto sobre políticas aplicadas nas próximas análises.

### 10. Camadas de Proteção

![Camadas de proteção](docs/images/10-camadas-protecao.png)

Texto modelo: O SafeLink combina diferentes mecanismos do Android para ampliar a cobertura, incluindo navegador padrão, compartilhamento, acessibilidade e VPN local.

### 11. Painel Terminal

![Painel terminal](docs/images/11-painel-terminal.png)

Texto modelo: O painel terminal oferece uma forma mais direta de inserir comandos, links e domínios, criando uma experiência técnica para testes e gerenciamento rápido.

### 12. Exportação e Backup

![Exportação e backup](docs/images/12-exportacao-backup.png)

Texto modelo: Os recursos de exportação e backup ajudam a preservar configurações, listas locais e relatórios, facilitando auditoria, compartilhamento e restauração de preferências.

## Escopo Atual

O SafeLink foca em reduzir o risco antes da abertura de links em dispositivos Android. Ele não substitui o modelo de segurança do Android, o isolamento do navegador, antivírus ou o julgamento do usuário.

Implementado na versão atual:

- Dashboard principal do SafeLink.
- Tela de revisão de links.
- Entrada como navegador seguro.
- Motor local de pontuação de URL.
- Serviço de proteção em foreground.
- Monitoramento opcional por acessibilidade.
- VPN local opcional para bloqueio DNS.
- Histórico e telas de detalhe.
- Exportação e backup de listas gerenciadas pelo usuário.
- Documentação técnica em `docs/`.

Fora do escopo desta versão:

- Sincronização em nuvem.
- Contas de usuário.
- API remota de reputação.
- Dashboard web.
- Pipeline de publicação na Play Store.

## Stack Técnica

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- AndroidX Core KTX
- Lifecycle Runtime
- Navigation Compose como dependência disponível
- SharedPreferences e persistência em JSON
- JUnit 4
- Robolectric
- Java 17

## Estrutura do Projeto

```text
.
|-- app/
|   |-- build.gradle.kts
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- assets/
|       |   |-- java/com/safelink/app/
|       |   |   |-- MainActivity.kt
|       |   |   |-- LinkReviewActivity.kt
|       |   |   |-- BrowserEntryActivity.kt
|       |   |   |-- data/
|       |   |   |-- model/
|       |   |   |-- security/
|       |   |   |-- service/
|       |   |   `-- ui/
|       |   `-- res/
|       |-- lite/
|       `-- test/
|-- docs/
|-- gradle/
|-- build.gradle.kts
|-- settings.gradle.kts
`-- README.md
```

## Requisitos

- Android Studio ou instalação compatível do Android SDK.
- JDK 17.
- Gradle Wrapper incluído neste repositório.
- Android SDK Platform 35.

## Como Começar

Clone o repositório:

```bash
git clone https://github.com/fjrsonn/SafeLink.git
cd SafeLink
```

Execute os testes unitários:

```bash
./gradlew testFullDebugUnitTest
```

Gere o APK debug da versão full:

```bash
./gradlew assembleFullDebug
```

Gere o APK debug da versão lite:

```bash
./gradlew assembleLiteDebug
```

No Windows PowerShell, use:

```powershell
.\gradlew.bat testFullDebugUnitTest
.\gradlew.bat assembleFullDebug
```

## Assinatura de Release

As credenciais de assinatura de release não são versionadas. Configure-as localmente por variáveis de ambiente ou pelo arquivo `local.properties`.

Variáveis de ambiente:

```bash
SAFELINK_RELEASE_STORE_FILE=path/to/safelink-release.keystore
SAFELINK_RELEASE_STORE_PASSWORD=sua-senha-do-keystore
SAFELINK_RELEASE_KEY_ALIAS=seu-alias
SAFELINK_RELEASE_KEY_PASSWORD=sua-senha-da-chave
```

Entradas equivalentes em `local.properties`:

```properties
safelink.release.storeFile=dist/safelink-release.keystore
safelink.release.storePassword=sua-senha-do-keystore
safelink.release.keyAlias=seu-alias
safelink.release.keyPassword=sua-senha-da-chave
```

Após configurar a assinatura, gere os artefatos de release:

```bash
./gradlew assembleFullRelease
./gradlew bundleFullRelease
```

APKs, AABs, keystores e saídas locais de build são ignorados pelo Git.

## Como a Análise Funciona

O analisador local atribui pontuação a diferentes sinais de risco:

- Ausência de HTTPS.
- Encurtadores de URL conhecidos.
- Punycode e riscos de domínios internacionalizados.
- Mistura de alfabetos parecidos.
- Domínios longos ou com formato incomum.
- Termos sensíveis comuns em golpes.
- Padrões de domínio que imitam marcas conhecidas.
- Uso direto de endereço IP.

O resultado é mapeado para um nível de risco:

- Seguro
- Suspeito
- Perigoso

Cada resultado inclui uma pontuação e motivos em linguagem clara para que o usuário entenda a decisão antes de continuar.

## Camadas de Proteção no Android

O SafeLink usa múltiplos mecanismos do Android porque nenhum método isolado cobre todos os aplicativos:

- Entrada como navegador ou app padrão: recebe links HTTP/HTTPS quando o Android direciona o fluxo para o SafeLink.
- Compartilhamento: revisa links enviados por outros aplicativos como texto.
- Serviço de acessibilidade: observa opcionalmente textos visíveis e abre a revisão quando detecta uma URL.
- VPN local: aplica bloqueio DNS por domínio com base em políticas locais.

Alguns aplicativos usam navegadores internos, renderizadores privados, DNS próprio, cache ou fluxos que limitam a interceptação. O SafeLink documenta essas limitações em vez de prometer cobertura total.

## Documentação

A pasta `docs/` contém documentação técnica mais profunda, incluindo:

- Visão de produto.
- Requisitos funcionais e não funcionais.
- Arquitetura.
- Fundação Android.
- Motor de interceptação.
- Motor de análise de URL.
- Segurança e privacidade.
- Testes e QA.
- Release e distribuição.
- Roadmap futuro.

Comece por:

- `docs/00_README.md`
- `docs/05_architecture.md`
- `docs/10_interception_engine.md`
- `docs/11_url_analysis_engine.md`
- `docs/16_testing_qa.md`

## Testes

Comando principal:

```bash
./gradlew testFullDebugUnitTest
```

Os testes unitários atuais cobrem casos representativos do analisador:

- Links de phishing com banco, login e senha.
- Links suspeitos que imitam marcas.
- Links encurtados.
- Links HTTP com termos sensíveis.
- Links seguros de documentação.

## Notas de Segurança

- Não versionar keystores de release.
- Não versionar senhas de assinatura.
- Não versionar `local.properties`.
- Tratar APKs e AABs gerados como artefatos de release, não como código-fonte.
- Revisar permissões e comportamento dos serviços Android antes de distribuir.

## Licença

Nenhum arquivo de licença foi incluído até o momento. Adicione uma licença antes de distribuir o projeto ou aceitar contribuições externas.
