# 📈 RootsSkyMarket
### O Banco Central e Corretora Dinâmica para seu Servidor de Skyblock (Paper 1.21.1)

---

**RootsSkyMarket** é um plugin de economia dinâmico e de alta performance desenvolvido para servidores Minecraft (Paper/Spigot 1.21.1). Ele funciona como o **Banco Central** e a **Corretora de Ações** do seu servidor, regulando a oferta e demanda, controlando a inflação e a deflação com precisão matemática, e oferecendo uma experiência imersiva e interativa de Bolsa de Valores aos jogadores.

O plugin é **plug-and-play** com banco de dados SQLite local, mas também oferece suporte nativo e robusto para **MySQL** e **PostgreSQL** (com pool HikariCP assíncrono), ideal para servidores com alta carga ou redes integradas.

---

## ✨ Características Principais

* 🛒 **Loja Dinâmica Premium (ShopGUI):** Menus elegantes e 100% seguros contra duplicação de itens. Os preços flutuam com base nas transações de compra e venda.
* 📊 **Bolsa de Valores (StockGUI):** Interface com as maiores altas (*Top Gainers*), maiores quedas (*Top Losers*), e a carteira de investimentos do próprio jogador.
* 🧮 **Motor Matemático Assíncrono:** Cálculos executados em segundo plano, aplicando decaimento de preço e impacto de volume nas tendências do mercado sem travar o servidor principal.
* 📦 **Modos de Venda Rápida & Avançada:**
  * Comando `/sellgui` (abre um menu onde você joga os itens e vende tudo de uma vez ao fechar).
  * Comandos `/vender tudo` (`/sellall`) e `/vender mao` (`/sellhand`).
  * Clique modificador no menu: `[Shift + Clique Esquerdo]` vende todo o tipo de item no inventário.
  * Transações com quantidade personalizada no chat de forma assíncrona.
* 🌍 **Banco de Dados Plug-and-Play:** Suporte nativo para **SQLite**, **MySQL** e **PostgreSQL**. Todas as consultas ao banco são assíncronas (`CompletableFuture`).
* 🔗 **Integração com Discord (Webhook):** Anuncie eventos de mercado como *Bull Market*, *Bear Market* ou o *Queridinho do Dia* diretamente em canais do seu Discord com alertas visuais.
* ⚡ **Eventos de Mercado:**
  * **Bull Market:** Taxas reduzidas no mercado.
  * **Bear Market:** Taxas de imposto dobradas e dividendos suspensos.
  * **Hot Stock (Queridinho do Dia):** Um item é selecionado para ter 0% de impostos nas transações por 24 horas.
* 💰 **Risco Orgânico (Dividendos e Custódia):** Jogadores recebem dividendos diários por manter ações de itens valorizados, ou pagam taxas de custódia se mantiverem ações de itens desvalorizados.
* 🛡️ **Medidas Anti-Monopólio:** Limite diário de vendas por jogador com suporte nativo a limites maiores para cargos VIPs via permissões configuráveis.
* 🎮 **Altamente Customizável:** Títulos de inventários, sons de cliques/sucessos, mensagens, formatação de moedas e parâmetros matemáticos são 100% configuráveis.

---

## 🛠️ Requisitos e Dependências

### Obrigatórios:
* **Paper / Purpur 1.21.1** (ou derivados do Spigot 1.21.1)
* **Java 21** ou superior
* [**Vault**](https://dev.bukkit.org/projects/vault) (para gerenciamento do saldo dos jogadores com seu plugin de economia preferido)

### Opcionais (Recomendados):
* [**PlaceholderAPI**](https://www.spigotmc.org/resources/placeholderapi.6245/) (para exibição de dados do mercado em scoreboards, tablists e hologramas)
* [**EconomyShopGUI**](https://www.spigotmc.org/resources/economyshopgui.69927/) (suporte a transições/softdepend)

---

## 💻 Comandos e Permissões

### Comandos de Jogador
| Comando | Descrição | Aliases | Permissão | Padrão |
| :--- | :--- | :--- | :--- | :--- |
| `/loja` | Abre o menu de Categorias e Compra/Venda da loja | - | *Nenhuma* | Todos |
| `/bolsa` | Abre a interface de Bolsa de Valores | `/stock`, `/mercado` | *Nenhuma* | Todos |
| `/tendencias` | Abre o painel direto de Top Gainers e Top Losers | - | *Nenhuma* | Todos |
| `/vender mao` | Vende o item que você está segurando | `/sellhand`, `/vender hand` | `rootssky.market.cmd.vender` | Op |
| `/vender tudo` | Vende todos os itens compatíveis do inventário | `/sellall`, `/vender all` | `rootssky.market.cmd.vender` | Op |
| `/sellgui` | Abre o painel de venda rápida para jogar itens dentro | - | `rootssky.market.cmd.vender` | Op |

### Comandos de Administração
| Comando | Descrição | Permissão | Padrão |
| :--- | :--- | :--- | :--- |
| `/bolsaadmin reload` | Recarrega as configurações do `config.yml` | `rootssky.market.admin` | Op |
| `/bolsaadmin reset` | Reseta a tabela de preços do banco de dados para os valores padrão | `rootssky.market.admin` | Op |
| `/bolsaadmin forceevent <bull/bear/normal>` | Força imediatamente um evento econômico no mercado | `rootssky.market.admin` | Op |
| `/bolsaadmin forcehotstock [item]` | Força um item (ou aleatório) a ser o *Queridinho do Dia* | `rootssky.market.admin` | Op |

---

## ⚙️ Instalação e Configuração Inicial

1. Baixe o arquivo `.jar` do plugin da nossa aba de **Releases** no GitHub.
2. Coloque o arquivo `.jar` na pasta `plugins` do seu servidor.
3. Inicie o servidor para gerar a pasta `plugins/RootsSkyMarket` e o arquivo `config.yml`.
4. Pare o servidor e edite o arquivo `config.yml` de acordo com as necessidades do seu servidor.
5. Se for usar **MySQL** ou **PostgreSQL**, configure a seção `database` alterando o `type` e adicionando as credenciais. Caso contrário, deixe como `SQLITE` para rodar localmente de forma automática.
6. Inicie o servidor novamente e divirta-se!

---

## 📄 Exemplo de Configuração (`config.yml`)

```yaml
# Configuração do Banco de Dados
# Tipos suportados: SQLITE, MYSQL, POSTGRESQL
database:
  type: "SQLITE"
  host: "seu-host.com"
  port: 5432
  database: "postgres"
  user: "usuario"
  password: "sua_senha_aqui"
  pool-size: 5

economy:
  transaction_tax_percent: 5.0
  price_update_interval_minutes: 5
  decay_rate_per_hour: 0.05
  volume_scale_divisor: 3.0
  max_volume_scale: 2.0
  currency:
    symbol: "R$"
    format: "{symbol}{amount}"

# Integração com o Discord
discord_webhook:
  enabled: false
  url: "https://discord.com/api/webhooks/SUA_URL_AQUI"
  bot_name: "Lobo de Wall Street"
  avatar_url: "https://minotar.net/helm/Notch/100.png"
```

---

## 🔒 Segurança e Robustez

* **Segurança em Inventários:** Todas as GUIs utilizam checagens de propriedades do inventário em eventos síncronos e impedem quaisquer cliques indesejados ou manipulação indevida de itens (imunes a exploits de duplicação).
* **Tratamento de Fechamento:** Se um jogador fechar o menu `/sellgui` ou o servidor sofrer queda/reinicialização, todos os itens válidos e aceitos são vendidos de forma síncrona/segura para o saldo Vault do jogador, e qualquer item inválido é devolvido com segurança ao inventário ou dropado no chão caso não haja espaço.
* **Consultas Assíncronas:** Acesso ao banco de dados nunca interfere na Thread Principal do Servidor, prevenindo picos de lag (*TPS drops*) comuns em plugins de economia dinâmicos antigos.

---

## 📦 Tags de Permissões VIP (Exemplo de Anti-Monopólio)

Por padrão, os jogadores comuns têm o limite diário definido em `anti_monopoly.max_sell_per_day`. Você pode dar permissões VIPs no seu plugin de permissões (como o LuckPerms) para dar limites maiores:
* `rootssky.market.vip.ascendente` (20.000 itens/dia)
* `rootssky.market.vip.ancestral` (40.000 itens/dia)
* `rootssky.market.vip.raizceleste` (800.000 itens/dia)

---

## 🤝 Contribuição e Bugs

Caso encontre algum bug ou tenha sugestões de melhorias durante esta fase **BETA**, abra uma **Issue** ou envie um **Pull Request** no nosso repositório no GitHub. Sua contribuição é de extrema importância para a evolução deste projeto!

---

*Desenvolvido com carinho por **Vinicius Carvalho**.*
