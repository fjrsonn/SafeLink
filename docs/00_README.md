# SafeLink - Documentação do Projeto

Atualizado em: 13/06/2026  
Versão documentada: 1.0.0 full release

Este diretório descreve o estado real do aplicativo Android SafeLink. A documentação foi revisada para remover referências a recursos que não existem na versão atual e para registrar apenas o que está implementado no código.

## Estado atual

SafeLink é um app Android nativo em Kotlin e Jetpack Compose que atua como navegador seguro, analisador local de links, monitoramento opcional por acessibilidade e VPN local de bloqueio DNS. A versão final está travada em modo retrato.

Artefatos finais:

- `dist/SafeLink-1.0.0-full-release.apk`
- `dist/SafeLink-1.0.0-full-release.aab`

Validação executada:

- `lintFullRelease`
- `testFullDebugUnitTest`
- `assembleFullRelease`
- `bundleFullRelease`
- APK verificado com `apksigner`
- 6 testes unitários aprovados

## Documentos principais

- `01_product_vision.md`: visão do produto.
- `02_requirements_functional.md`: requisitos funcionais implementados.
- `03_requirements_nonfunctional.md`: requisitos não funcionais.
- `05_architecture.md`: arquitetura real do app.
- `06_tech_stack.md`: tecnologias usadas.
- `10_interception_engine.md`: mecanismos de interceptação.
- `11_url_analysis_engine.md`: motor local de análise.
- `16_testing_qa.md`: testes e validação.
- `17_release_distribution.md`: distribuição e instalação.

## Observação

O SafeLink reduz risco antes da abertura de links, mas não substitui o modelo de segurança do Android. Alguns apps podem usar navegadores internos, DNS próprio, cache, renderização privada ou fluxos que impedem interceptação completa.