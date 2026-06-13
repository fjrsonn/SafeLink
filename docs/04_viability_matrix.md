# Matriz de Viabilidade

| Recurso | Viabilidade na versão 1.0.0 | Observação |
| --- | --- | --- |
| Análise local de URL | Implementado | Baseado em heurísticas locais. |
| Tela de decisão | Implementado | Exibe risco, score, motivos e ações. |
| Histórico | Implementado | Persistido em SharedPreferences como JSON. |
| SafeLink como navegador padrão | Implementado | Depende de configuração do usuário no Android. |
| Monitoramento de chats | Implementado parcialmente | Depende de AccessibilityService e do conteúdo exposto por outros apps. |
| VPN local DNS | Implementado | Bloqueia por domínio; não lê URL completa HTTPS. |
| Listas de confiança/bloqueio | Implementado | Locais, sem sincronização. |
| QR/câmera | Implementado | Abre câmera por intent. |
| Dashboard | Implementado | Usa dados locais do histórico. |
| Publicação pública | Não executada | Distribuição atual por APK/AAB local. |

## Conclusão

A versão 1.0.0 é viável como app Android local de proteção preventiva. A cobertura total depende de permissões e limites do sistema Android.