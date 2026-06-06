# 📦 RootsSkyMarket - Versão v1.0.3-BETA

Bem-vindo à atualização **v1.0.3-BETA** do **RootsSkyMarket**! Esta versão traz melhorias críticas de usabilidade, estabilidade e recursos premium voltados para a substituição completa e definitiva do *EconomyShopGUI* no seu servidor Skyblock.

---

## ✨ O Que Há de Novo?

| Categoria | Recurso | Descrição |
| :--- | :--- | :--- |
| 🛡️ **Anti-Monopólio** | `limits.yml` Dedicado | Arquivo separado mastigadinho com limites individuais ativáveis para dezenas de itens padrão. |
| 💎 **Economia** | Bônus de Venda VIP | Multiplicadores de venda com destaque visual riscado (`§m`) nas loras das GUIs. |
| 🧭 **Navegação** | NPC Voltar Lock | Opção para trancar a navegação e ocultar o botão voltar para menus abertos via NPCs. |
| ⚙️ **CI/CD** | Pipeline Corrigido | Sincronização automática de builds de jar para o release do GitHub. |

---

## 🛠️ Detalhes das Implementações

### 📄 Novo arquivo de limites: `limits.yml`
Esqueça a necessidade de preencher IDs manualmente. Agora o plugin gera o arquivo `limits.yml` dedicado contendo os limites diários padrão de todos os principais blocos, minérios, plantas e alimentos do jogo.
Você pode ligar ou desligar limites individuais de forma prática:
```yaml
limits:
  DIAMOND:
    enabled: true
    limit: 500
  EMERALD:
    enabled: false  # Usa o limite geral do config.yml
    limit: 500
```
> [!TIP]
> Os limites dos jogadores VIPs cadastrados no `config.yml` sob `anti_monopoly.vip_limits` continuam sendo escalados de forma proporcional e automática para todos os itens ativados!

---

### 💳 Destaque Visual de Vendas VIP (Preço Riscado)
Seguindo o estilo premium de outros plugins de economia de mercado, os jogadores VIP agora enxergam na lore dos itens o preço base original com efeito riscado (`§m`) e o valor de venda com o buff VIP destacado ao lado.
* **Exemplo Visual no Menu:**
  `Preço de Venda: §7§mR$ 10,00§r §aR$ 12,50 §e(+25% VIP)`

---

### 🔓 Bloqueio de Navegação para NPCs
Ao configurar lojas temáticas associadas a NPCs, você pode usar o novo argumento `/loja <categoria> esconder`.
* Substitui o botão voltar por vidro preto.
* Desativa todas as formas de voltar ao menu global de categorias, garantindo que o jogador permaneça restrito à loja que o NPC abriu.

---

## 💾 Instruções de Atualização
1. Substitua o antigo arquivo jar por `RootsSkyMarket-1.0.3-BETA.jar` na sua pasta `plugins/`.
2. Reinicie o servidor ou execute `/bolsaadmin reload` para que o novo arquivo `limits.yml` seja criado automaticamente.
3. Edite o `limits.yml` de acordo com a economia do seu servidor!

---
*Desenvolvido com carinho e foco em performance pela equipe RootsSky.* ❤️
