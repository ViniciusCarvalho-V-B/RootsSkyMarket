package com.rootssky.market.utils;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    public static void send(RootsSkyMarket plugin, String content) {
        if (!plugin.getConfig().getBoolean("discord_webhook.enabled", false)) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString("discord_webhook.url");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("SUA_URL_AQUI")) {
            return;
        }

        String botName = plugin.getConfig().getString("discord_webhook.bot_name", "Lobo de Wall Street");
        String avatarUrl = plugin.getConfig().getString("discord_webhook.avatar_url", "");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                // Escape quotes for json
                String safeContent = content.replace("\"", "\\\"");
                String safeName = botName.replace("\"", "\\\"");
                
                String jsonInputString = "{\"content\": \"" + safeContent + "\", \"username\": \"" + safeName + "\"";
                if (!avatarUrl.isEmpty()) {
                    jsonInputString += ", \"avatar_url\": \"" + avatarUrl + "\"";
                }
                jsonInputString += "}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao enviar webhook do Discord: " + e.getMessage());
            }
        });
    }
}
