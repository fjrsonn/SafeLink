# Requisitos Não Funcionais

## Plataforma

- Android minSdk 29.
- Android targetSdk 35.
- Kotlin e Jetpack Compose.

## Privacidade

- Sem backend remoto na versão 1.0.0.
- Histórico, listas e preferências ficam no armazenamento privado do app.
- O app não envia URLs para servidor externo próprio.

## Segurança

- Cleartext traffic bloqueado por `network_security_config`.
- APK release assinado com keystore local do projeto.
- VPN local usa permissão explícita do Android.
- AccessibilityService exige ativação manual pelo usuário.

## Usabilidade

- Interface escura.
- Navegação por botões fixos inferiores.
- Tela travada em retrato.
- Linguagem voltada a decisão rápida.

## Confiabilidade

- Histórico persiste após fechar o app.
- Links já resolvidos por ação do usuário são marcados para evitar reavisos automáticos em sequência pelo monitoramento de acessibilidade.

## Limitações aceitas

- O app não garante interceptação absoluta de todos os links do sistema.
- Apps com navegador interno podem abrir links sem passar pelo SafeLink.
- DNS-over-HTTPS próprio, cache DNS ou IP direto podem reduzir a cobertura da VPN local.