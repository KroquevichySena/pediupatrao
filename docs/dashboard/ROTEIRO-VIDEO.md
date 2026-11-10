# Roteiro do vídeo — até 3 minutos

## Antes de gravar

1. Suba o MongoDB e a aplicação com perfil `demo`, conforme o README. Abra `/login`, entre como `gerente01` e acesse `/dashboard`.
2. Escolha os últimos 28 dias para mostrar toda a massa fictícia. Deixe o repositório aberto em outra aba e os testes disponíveis no terminal.
3. Grave a tela e a voz. Meta: 2 min 45 s, com 15 s de margem. Não mostre senhas ou a conexão do Atlas.

| Tempo | O que mostrar | Fala sugerida |
|---|---|---|
| 0:00–0:20 | Dashboard com números e gráficos | “Este é o dashboard administrativo do Pediu Patrão, integrado ao sistema de pedidos que desenvolvemos nas atividades anteriores. Ele ajuda o responsável pelo estabelecimento a acompanhar as vendas e a operação. Os dados desta demonstração são fictícios e estão em um banco separado.” |
| 0:20–0:45 | Quatro indicadores e descontos/cancelamentos | “Aqui temos faturamento, quantidade de pedidos, ticket médio e pedidos em andamento. O faturamento considera somente pedidos finalizados, após descontos. Cancelamentos aparecem separados, para não inflar a receita. O ticket médio é calculado pela API.” |
| 0:45–1:15 | Alterar período, modalidade e agrupamento; aplicar | “Os filtros de período, situação e modalidade atualizam todos os componentes juntos. Podemos alternar a evolução das vendas entre dias e meses. Os dias sem vendas aparecem com zero, evitando uma leitura distorcida da tendência.” |
| 1:15–1:40 | Gráfico de status, ranking e pedidos recentes | “A distribuição por situação ajuda a identificar acúmulo de pedidos. O ranking mostra os produtos mais vendidos por quantidade, e a tabela permite acompanhar os pedidos recentes com paginação. Há atualização manual e automática a cada minuto.” |
| 1:40–2:10 | Aba com `/api/dashboard` autenticada e arquivos do projeto | “A interface consome uma API REST em Spring Boot. Um endpoint consolida os dados do painel, e outro fornece os filtros. A consulta usa os pedidos persistidos no MongoDB. Mantivemos a arquitetura existente e separamos controller, serviço e DTO, sem duplicar dados em outra coleção.” |
| 2:10–2:35 | Testes e documentação no GitHub | “O acesso é restrito a administrador e gerente. Os testes verificam cálculos, filtros, permissões e erros; também há testes com MongoDB real, incluindo os limites das datas. O repositório contém requisitos, documentação dos endpoints, evidências e branches seguindo Gitflow.” |
| 2:35–2:45 | Voltar ao painel | “Assim, o gestor consegue acompanhar o desempenho e identificar tendências usando informações atualizadas do próprio sistema de pedidos.” |

## Finalização da entrega

- Salve o vídeo como MP4 e adicione-o ao repositório em `docs/dashboard/video/` (até 3 minutos e abaixo do limite de arquivo do GitHub).
- A gravação deve ser feita por você; o roteiro não substitui o vídeo solicitado.
- No Teams, envie apenas o link do repositório, conforme o enunciado.
