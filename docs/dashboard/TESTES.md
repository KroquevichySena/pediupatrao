# Evidências de testes — Dashboard Administrativo

Execução realizada em **10/11/2026**, Windows 11, JDK Azul 21.0.6, Maven 3.9.9, Spring Boot 3.4.4 e MongoDB 8.0.5 real, local e isolado. A aplicação usou o perfil `demo`; os testes de integração usaram `pediupatrao_dashboard_test`.

## Resultado da suíte automatizada

Comando equivalente ao utilizado (o ambiente de validação usou ferramentas portáteis e cache Maven fora do repositório):

```powershell
$env:RUN_MONGO_TESTS = "true"
$env:MONGODB_URI = "mongodb://localhost:27018"
.\mvnw.cmd verify
```

**48 testes executados; zero falhas; zero erros; zero ignorados. BUILD SUCCESS.** O processo também compilou e gerou `target/pediupatrao-1.0-SNAPSHOT.jar`.

| Classe | Testes | Resultado |
|---|---:|---|
| DashboardApiTest | 8 | Passaram |
| DashboardMongoIntegrationTest | 2 | Passaram com MongoDB real |
| DashboardServiceTest | 6 | Passaram |
| PedidoApiSegurancaTest | 7 | Passaram |
| TelasRenderizacaoTest | 4 | Passaram |
| StatusPedidoTest | 4 | Passaram |
| AuditoriaServiceTest | 4 | Passaram |
| PedidoServiceTest | 13 | Passaram |

Relatórios textuais originais do Surefire: [evidencias/surefire](evidencias/surefire/). Execução resumida: [maven-verify.txt](evidencias/maven-verify.txt).

## Casos cobertos

- Receita líquida: dois finalizados de R$ 90 + R$ 50 resultam em R$ 140, ticket R$ 70 e desconto R$ 10; pedido cancelado de R$ 999 e recebido de R$ 300 não entram na receita.
- Ranking: três unidades de um mesmo produto são consolidadas, com valor bruto de R$ 150 antes dos descontos.
- Situações: total coerente com a soma dos estados, inclusive estado ausente do legado.
- Períodos sem pedidos: receita e ticket zero, série preenchida com zero, ranking vazio, sem divisão por zero.
- Datas: MongoDB inclui 01/02/2024 às 00:00 e 29/02/2024 às 23:59:59, e exclui 01/03/2024 às 00:00.
- Filtros combinados de status e retirada aplicados no banco.
- Paginação: 23 pedidos geram três páginas; a última contém três pedidos.
- Autorização: 401 sem login; 403 para ATENDENTE; ADMIN e GERENTE acessam; escritas no dashboard bloqueadas.
- Falhas: parâmetros malformados retornam 400, regras inválidas 422, erro do banco 503 sem expor a mensagem técnica.
- Renderização: template Thymeleaf do dashboard e telas anteriores renderizam com Spring MVC.

## Teste de fumaça HTTP com servidor real

Além dos testes MockMvc, o JAR gerado foi iniciado em `127.0.0.1:8080`, conectado ao MongoDB em `127.0.0.1:27018`. Executou-se:

```powershell
python scripts/smoke-dashboard.py http://127.0.0.1:8080
```

O script usa a biblioteca padrão do Python, faz login real com cookie de sessão e token CSRF, e verifica:

1. API sem sessão retorna 401.
2. GERENTE acessa template, JavaScript, CSS e Chart.js locais com HTTP 200.
3. JSON real apresenta indicadores, série de 28 dias e soma dos status consistentes.
4. Período vazio retorna HTTP 200 e zeros.
5. Filtros inválidos retornam 400/422.
6. ATENDENTE autenticado recebe 403 na API e página.
7. ADMIN autenticado acessa as opções de filtros.

Saída capturada: [smoke-http.txt](evidencias/smoke-http.txt).

## Verificação adicional e limites

- `node --check src/main/resources/static/js/dashboard.js`: passou.
- `git diff --check`: passou na preparação da entrega.
- O controle visual do navegador foi interrompido pela ferramenta porque ela não conseguiu identificar a URL atual com confiança. Por isso não são apresentadas capturas nem se afirma teste visual completo. Confira disposição no celular, interação dos gráficos e atualização automática ao preparar a gravação.
- Dockerfile e Compose foram preparados, mas não executados neste ambiente. Java, MongoDB, compilação, integração e HTTP reais foram verificados diretamente.
- Não houve teste de carga, auditoria de segurança completa nem teste de produção/Atlas. A solução é uma entrega acadêmica integrada ao modelo existente.

## Reprodução no GitHub

O workflow [tests.yml](../../.github/workflows/tests.yml) sobe MongoDB e executa `verify` com integração habilitada. Os relatórios são enviados como artefato da execução. O resultado da CI deve ser consultado na aba Actions; o resultado local acima não presume sucesso da execução remota.

A primeira execução remota, em `ubuntu-latest`, parou na inicialização do container: a versão do MongoDB recusou o kernel Linux do runner antes de qualquer teste. O workflow foi ajustado para `ubuntu-22.04`. A incompatibilidade é descrita nas [notas oficiais do MongoDB 8.0](https://www.mongodb.com/docs/v8.0/release-notes/8.0/). O histórico de execuções permanece na aba Actions.
