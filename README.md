# PediuPatrão 🍕 — Controle de acesso, gestão de pedidos e auditoria

Evolução do projeto PediuPatrão (Java 21, Spring Boot 3.4, Spring Security, MongoDB e Thymeleaf) para a Atividade Prática M1 — Parte 2 de Engenharia de Software.

## Como executar

**Pré-requisitos:** JDK 21 ou mais novo e um banco no MongoDB Atlas (ou um MongoDB local).

1. Configure a conexão com o banco pela variável de ambiente `MONGODB_URI`:

   ```powershell
   $env:MONGODB_URI = "mongodb+srv://<usuario>:<senha>@<cluster>.mongodb.net/?appName=PediuPatrao"
   ```

   Sem essa variável, a aplicação tenta `mongodb://localhost:27017`. O nome do banco padrão é `pizzaria` (pode ser trocado com `MONGODB_DATABASE`).

2. Rode a aplicação:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. Acesse http://localhost:8080.

Na primeira execução são criados um usuário de cada perfil e alguns produtos de exemplo:

| Usuário | Perfil |
|---|---|
| `admin` | ADMIN |
| `gerente01` | GERENTE |
| `atendente01` | ATENDENTE |

A senha inicial vem da propriedade `app.seed.senha-inicial` (variável `APP_SENHA_INICIAL`). O valor padrão está em `application.properties` e deve ser trocado fora do ambiente de desenvolvimento.

**Testes:** `.\mvnw.cmd test` (não precisam de banco de dados).

## Requisitos implementados

### RF01 — Usuários e permissões
- Três perfis (enum `Perfil`): **ADMIN**, **GERENTE** e **ATENDENTE**. O `UsuarioDetailsService` passou a usar o perfil gravado no usuário, em vez de atribuir `USER` a todos. Usuários antigos sem perfil válido recebem o perfil de menor privilégio.
- A autorização tem duas camadas no backend:
  1. regras por URL no `SecurityConfig`;
  2. `@PreAuthorize` nos controllers e nos services (`@EnableMethodSecurity`).

  Esconder botões na tela é só conveniência.

| Operação | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Gerenciar usuários e perfis | ✅ | | |
| Consultar pedidos | ✅ | ✅ | ✅ |
| Registrar pedido | | | ✅ |
| Avançar status do pedido | | ✅ | ✅ |
| Aplicar desconto / cancelar pedido | | ✅ | |
| Consultar clientes | | ✅ | ✅ |
| Cadastrar / alterar clientes | | | ✅ |
| Excluir cliente | | ✅ | |
| Consultar auditoria | ✅ | ✅ | |

- A API `/api/usuarios`, que era pública (`permitAll`), agora é exclusiva do ADMIN.
- Proteção contra autoelevação e erros de administração:
  - o perfil é validado contra o enum;
  - o ADMIN não pode alterar o próprio perfil ou login nem excluir a si mesmo;
  - o sistema nunca fica sem nenhum ADMIN;
  - o hash da senha nunca é devolvido no JSON.
- O CSRF foi reativado:
  - formulários Thymeleaf recebem o token automaticamente;
  - chamadas `fetch` enviam o token pelo cabeçalho, lido das meta tags do `layout.html`;
  - o logout passou a ser feito por POST.
- Acesso sem login às APIs recebe `401`. Acesso com perfil errado recebe `403`, com mensagem JSON padronizada (`ApiExceptionHandler`).

### RF02 — Cadastro de pedidos
- O `Pedido` passou a ter:
  - itens (`ItemPedido`: produto, quantidade, preço unitário e subtotal);
  - valor dos itens, percentual e valor do desconto e valor total;
  - tipo (entrega ou retirada) e observação;
  - responsável e data/hora de entrada.
- A requisição (`NovoPedidoRequest`) só aceita cliente, tipo, observação e lista de `{produtoId, quantidade}`. **Preços e total são calculados no servidor** a partir do cadastro de produtos. Um campo `valorTotal` enviado no JSON é ignorado.
- Validações:
  - o cliente precisa existir;
  - o pedido precisa ter pelo menos um item;
  - a quantidade vai de 1 a 50;
  - o produto precisa existir, estar ativo e ter preço válido.

### RF03 — Entrada, saída e transições
- Fluxo definido no enum `StatusPedido`: `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA | RETIRADO → FINALIZADO`.
- Transições fora do fluxo são recusadas, por exemplo finalizar um pedido em preparação. Também são recusados:
  - pedido de retirada "saindo para entrega" e vice-versa;
  - qualquer mudança depois do cancelamento.
- Na entrada são gravados o usuário responsável e a data/hora. Na saída (`SAIU_PARA_ENTREGA` ou `RETIRADO`) são gravados obrigatoriamente o usuário e a data/hora da saída.
- Cada mudança fica no histórico do pedido.

### RF04 — Cancelamentos e descontos (somente GERENTE)
- **Cancelamento:**
  - exige justificativa de pelo menos 10 caracteres;
  - só é permitido antes da saída;
  - mantém itens e valores e registra quem cancelou, quando e o status anterior;
  - depois dele, nenhuma outra alteração é aceita.
- **Desconto:**
  - é percentual, de 0% a 20%, e só pode ser aplicado antes da saída;
  - o total é recalculado no servidor;
  - aplicar de novo substitui o percentual anterior, sem acumular.
- As duas operações são auditadas com usuário, data/hora e valores anteriores e novos.

### RF05 — Auditoria
- Componentes próprios:
  - entidade `RegistroAuditoria` (coleção `auditoria`);
  - repositório `RegistroAuditoriaRepository`;
  - serviço `AuditoriaService`.
- São auditados:
  - inclusão de pedido, mudança de status, desconto e cancelamento;
  - inclusão, alteração e exclusão de cliente, campo a campo (endereço, telefone etc.);
  - inclusão, alteração e exclusão de usuários.
- Cada registro guarda:
  - usuário autenticado e perfil;
  - data/hora e tipo de operação;
  - entidade e id;
  - lista de campos alterados com valor anterior e novo;
  - justificativa, quando houver.
- Integridade:
  - o repositório estende `Repository` (e não `MongoRepository`), então não existem métodos de atualização ou exclusão;
  - não há endpoints de escrita: `POST`, `PUT` e `DELETE` em `/auditoria` e `/api/auditoria` são negados;
  - a consulta (`/auditoria` e `GET /api/auditoria`) é exclusiva de ADMIN e GERENTE.
- Proteção de dados:
  - senhas nunca são gravadas (o campo aparece como `[protegido]`);
  - telefones aparecem mascarados na consulta (só os 4 últimos dígitos);
  - na exclusão de cliente é guardado só o nome;
  - senhas não são escritas em log.

## Principais alterações

| Camada | Arquivos |
|---|---|
| Configuração | `SecurityConfig` (regras por perfil, CSRF, 401 nas APIs), `DataInitializer` (usuários por perfil, produtos, correção de perfis antigos), `application.properties` (credenciais fora do código) |
| Entidades | `Pedido`, `ItemPedido`, `MovimentacaoPedido`, `StatusPedido`, `TipoEntrega`, `Perfil`, `RegistroAuditoria`, `AlteracaoCampo`, `TipoOperacao` |
| Serviços | `PedidoService`, `ClienteService`, `UsuarioService`, `UsuarioDetailsService`, `ProdutoService`, `AuditoriaService` (novo) |
| Controllers | `PedidoController` (API), `PedidoWebController` (telas), `AuditoriaController` e `AuditoriaWebController` (novos), `ClienteController`, `UsuarioController`, `ProdutoController`, `HomeController` |
| Telas | `pedidos.html`, `pedidos/form.html`, `pedidos/detalhe.html`, `auditoria/lista.html`, menu por perfil, CSRF nas chamadas `fetch`, página de produtos corrigida |
| Testes | `StatusPedidoTest`, `PedidoServiceTest`, `AuditoriaServiceTest`, `PedidoApiSegurancaTest`, `TelasRenderizacaoTest` |

## API — principais endpoints

| Método | Rota | Perfil |
|---|---|---|
| GET | `/api/pedidos`, `/api/pedidos/{id}` | todos |
| POST | `/api/pedidos` | ATENDENTE |
| PATCH | `/api/pedidos/{id}/status` — `{"status":"EM_PREPARACAO"}` | ATENDENTE, GERENTE |
| POST | `/api/pedidos/{id}/desconto` — `{"percentual":10}` | GERENTE |
| POST | `/api/pedidos/{id}/cancelamento` — `{"justificativa":"..."}` | GERENTE |
| GET | `/api/auditoria?entidade=PEDIDO&entidadeId=...` | ADMIN, GERENTE |

**Testando um bloqueio pelo navegador:** logado como `atendente01`, abra o console (F12) em qualquer página do sistema e rode:

```js
await apiFetch('/api/pedidos/<ID_DO_PEDIDO>/cancelamento', 'POST', { justificativa: 'teste de bloqueio' })
```

A resposta é `status: 403`, mesmo sem nenhum botão na tela.
