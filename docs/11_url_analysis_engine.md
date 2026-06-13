# Motor de Análise de URLs

## Classe

`UrlAnalyzer` em `app/src/main/java/com/safelink/app/security/UrlAnalyzer.kt`.

## Entrada

Uma string de URL ou domínio. Caso não tenha protocolo, o analisador assume `https://`.

## Saída

`AnalysisResult` com:

- URL normalizada.
- Host.
- Nível de risco.
- Score de 0 a 100.
- Motivos explicativos.
- Decisão.
- Horário.
- Ocorrências/eventos.

## Regras locais implementadas

- Ausência de HTTPS.
- Encurtadores conhecidos.
- Punycode.
- Caracteres internacionais e risco de homógrafo.
- Mistura de alfabetos.
- Domínio muito longo ou com muitos hífens.
- Palavras comuns em golpes.
- Marcas sensíveis em domínio/subdomínio.
- Lookalike simples.
- IP direto.

## Classificação

- `Safe`: score menor que 25.
- `Suspicious`: score entre 25 e 49.
- `Dangerous`: score maior ou igual a 50.

## Limites

- Não consulta reputação online.
- Não executa sandbox remoto.
- Não substitui análise humana ou solução corporativa de threat intelligence.