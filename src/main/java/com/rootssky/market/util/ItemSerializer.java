package com.rootssky.market.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemSerializer {

    private static final Logger LOGGER = Bukkit.getLogger();

    public static String toItemBase64(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Cannot serialize null or AIR ItemStack");
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(item.getType().ordinal());
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize ItemStack: " + item.getType(), e);
            throw new RuntimeException("Failed to serialize ItemStack to Base64", e);
        }
    }

    public static ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Cannot deserialize null or empty Base64 string");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            dataInput.readInt();
            return (ItemStack) dataInput.readObject();

        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize ItemStack from Base64", e);
            throw new RuntimeException("Failed to deserialize ItemStack from Base64", e);
        }
    }
}
