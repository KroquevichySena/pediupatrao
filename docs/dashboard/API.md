# API do Dashboard

Base local: `http://localhost:8080`. Todos os endpoints são GET e exigem sessão Spring Security (cookie JSESSIONID) de ADMIN ou GERENTE. Login pelo formulário `/login`, que recebe token CSRF. Não há autenticação Basic ou Bearer implementada. Use o navegador autenticado ou `fetch` na página.

## GET /api/dashboard

Retorna indicadores, série temporal, situações, ranking e página de pedidos em uma única consulta ao banco.

| Parâmetro | Tipo | Padrão / regra |
|---|---|---|
| inicio | data ISO YYYY-MM-DD | Primeiro dia do mês atual em São Paulo. |
| fim | data ISO YYYY-MM-DD | Hoje em São Paulo, inclusivo. |
| status | enum opcional | RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO ou CANCELADO. Ausente: todos. |
| tipoEntrega | enum opcional | ENTREGA ou RETIRADA. Ausente: ambas. |
| agrupamento | string | dia (padrão) ou mes. |
| limite | inteiro | Quantidade de produtos do ranking, de 1 a 20; padrão 5. |
| pagina | inteiro | Base zero, 0 a 100000; padrão 0. Tamanho fixo de 10 pedidos. |

Período entre 1 e 366 dias inclusivos. Parâmetros opcionais devem ser omitidos quando não utilizados. Se só uma data for enviada, o outro limite mantém seu padrão e o intervalo resultante é validado. Valores de enum são sensíveis a maiúsculas.

Requisição de exemplo, no console do navegador autenticado:

```javascript
fetch('/api/dashboard?inicio=2024-02-01&fim=2024-02-29&agrupamento=mes&tipoEntrega=RETIRADA')
  .then(async r => ({status: r.status, dados: await r.json()})).then(console.log);
```

Resposta 200 ilustrativa, compatível com a massa do teste de integração:

```json
{
  "inicio": "2024-02-01", "fim": "2024-02-29", "atualizadoEm": "2026-11-10T18:00:00",
  "indicadores": {
    "totalPedidos": 1, "finalizados": 1, "emAndamento": 0, "cancelados": 0,
    "faturamento": 90.00, "ticketMedio": 90.00, "descontos": 10.00, "taxaCancelamento": 0.00
  },
  "vendas": [{"periodo": "2024-02", "faturamento": 90.00, "pedidos": 1}],
  "situacoes": [
    {"status":"RECEBIDO","descricao":"Recebido","quantidade":0},
    {"status":"EM_PREPARACAO","descricao":"Em preparação","quantidade":0},
    {"status":"PRONTO","descricao":"Pronto","quantidade":0},
    {"status":"SAIU_PARA_ENTREGA","descricao":"Saiu para entrega","quantidade":0},
    {"status":"RETIRADO","descricao":"Retirado","quantidade":0},
    {"status":"FINALIZADO","descricao":"Finalizado","quantidade":1},
    {"status":"CANCELADO","descricao":"Cancelado","quantidade":0}
  ],
  "produtos": [{"produtoId":"pizza-it","nome":"Pizza Teste","quantidade":2,"valorBruto":100.00}],
  "pedidos": {
    "itens": [{"id":"it-dashboard-b","dataEntrada":"2024-02-29T23:59:59","status":"FINALIZADO","tipoEntrega":"RETIRADA","valorTotal":90.00}],
    "pagina":0,"tamanho":10,"total":1,"totalPaginas":1
  }
}
```

Valores monetários são números JSON em reais, taxas são percentuais de 0 a 100. Não use ponto flutuante do cliente para recalcular valores contábeis. `vendas.pedidos` conta apenas finalizados. `pedidos.total` conta todos os pedidos filtrados. Uma página além do total retorna `itens: []`. Uma consulta sem pedidos retorna 200 com indicadores zero, situações zero, ranking vazio e série preenchida com zero.

## GET /api/dashboard/filtros

Sem parâmetros. Exemplo:

```javascript
fetch('/api/dashboard/filtros').then(r => r.json()).then(console.log);
```

Resposta 200:

```json
{
  "status": [
    {"valor":"RECEBIDO","descricao":"Recebido"},
    {"valor":"EM_PREPARACAO","descricao":"Em preparação"},
    {"valor":"PRONTO","descricao":"Pronto"},
    {"valor":"SAIU_PARA_ENTREGA","descricao":"Saiu para entrega"},
    {"valor":"RETIRADO","descricao":"Retirado"},
    {"valor":"FINALIZADO","descricao":"Finalizado"},
    {"valor":"CANCELADO","descricao":"Cancelado"}
  ],
  "tiposEntrega":["ENTREGA","RETIRADA"],
  "agrupamentos":["dia","mes"],"limiteMaximo":20,"periodoMaximoDias":366
}
```

## Erros

| HTTP | Quando | Exemplo |
|---|---|---|
| 400 | Data, enum ou inteiro com formato inválido | `?inicio=ontem`, `?status=ABC`, `?pagina=abc` |
| 401 | Sem sessão autenticada ou sessão expirada | Consultar API antes do login. |
| 403 | ATENDENTE ou método de escrita | GET com perfil inadequado ou POST. |
| 422 | Regra de filtro inválida | Início após fim, mais de 366 dias, `agrupamento=semana`, `limite=21`, `pagina=-1`. |
| 503 | Falha de acesso ao MongoDB | Banco indisponível; detalhes técnicos ficam nos logs. |

Formato de erro produzido pelo tratamento REST (400/422/503 e 403 que alcance o controller):

```json
{"status":422,"erro":"Unprocessable Entity","mensagem":"Informe um período de 1 a 366 dias, com início até o fim.","dataHora":"2026-11-10T18:00:00"}
```

401 e 403 bloqueados no filtro de segurança podem vir sem esse corpo JSON; o cliente interpreta primeiro o código HTTP. O endpoint de filtros não consulta o banco, portanto continua disponível com sessão válida mesmo se o MongoDB cair.

## Página

`GET /dashboard`: HTML Thymeleaf, ADMIN/GERENTE. Sem login redireciona a `/login`; perfil inadequado recebe 403. CSS, JavaScript e Chart.js são servidos localmente. O layout legado ainda usa Bootstrap e ícones via CDN.
