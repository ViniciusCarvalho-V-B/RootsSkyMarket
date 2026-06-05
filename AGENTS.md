# RootsSkyMarket - Dynamic Economy Core & Shop (Paper 1.21.1)

## 🎯 Escopo do Projeto (O Banco Central e Corretora)
O `RootsSkyMarket` é o **Motor Econômico Dinâmico e a Loja Oficial (Corretora)** do servidor Skyblock. 
Ele gerencia a economia do servidor vendendo e comprando itens diretamente dos jogadores através de Menus (GUIs), calcula a inflação/deflação baseada em oferta e demanda e aplica os novos preços dinamicamente.

## 🏗️ Arquitetura e Integrações
* **Motor de Loja Nativo:** O plugin possui seus próprios menus (Categorias, Compra/Venda, Bolsa de Valores).
* **Economia Base:** `Vault` (Ponte de comunicação para adicionar/remover o saldo dos jogadores).
* **Banco de Dados:** MySQL (HikariCP).
* **Moedas Suportadas:** Formatação configurável para BRL (R$), USD ($) e EUR (€).

## ⚙️ Mecânicas Principais
1.  **Loja Dinâmica (ShopGUI):** Menus de loja altamente otimizados e seguros onde jogadores compram/vendem itens. Os preços na lore são atualizados dinamicamente incluindo setas de tendência.
2.  **Motor Matemático (EconomyEngine):** Roda assincronamente a cada X minutos. Calcula o novo preço dos itens com base no volume (compras aumentam o preço, vendas diminuem) e aplica uma taxa de decaimento.
3.  **Bolsa de Valores (StockGUI):** Interface gráfica que exibe as maiores altas (Top Gainers), maiores quedas (Top Losers) e a carteira de investimentos do jogador.

## 🗄️ Estrutura de Banco de Dados (MySQL)
O plugin gerencia duas tabelas essenciais:
* `market_prices`: Armazena o estado de cada item (item_id, base_price, current_price, alpha, floor_price, ceiling_price, volume_24h, last_update).
* `market_transactions`: Log histórico para auditoria (player_uuid, item_id, type, amount, unit_price, timestamp).

## 🛑 REGRAS E RESTRIÇÕES ESTRITAS PARA A IA
* **PROIBIDO P2P:** NUNCA crie, sugira ou implemente sistemas de venda entre jogadores ou leilões.
* **SEGURANÇA E PERFORMANCE (PRIORIDADE MÁXIMA):** Os menus de loja (`InventoryClickEvent`) devem ser imunes a duplicação. Verifique espaço no inventário antes de dar itens, cancele todos os cliques indesejados, processe transações no Vault com segurança.
* **PROIBIDO MANIPULAR ENTIDADES:** NUNCA crie, spawne ou gerencie NPCs ou Villagers diretamente. O servidor usa `Citizens` e os comandos da loja (`/loja`) serão atrelados a eles pelo dono do servidor.
* **PROIBIDO SERIALIZAÇÃO BASE64:** Não armazene itens serializados no banco de dados. Nós rastreamos apenas o `item_id` (String) e o preço (BigDecimal).
* **Performance:** Queries de banco de dados (`DatabaseManager`) devem ser sempre executadas de forma **assíncrona** (`CompletableFuture`).