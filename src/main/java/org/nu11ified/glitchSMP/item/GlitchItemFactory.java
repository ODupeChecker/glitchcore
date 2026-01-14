package org.nu11ified.glitchSMP.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchResourcePackRegistry;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates and identifies glitch items with custom model data.
 */
public class GlitchItemFactory {
    private static final Material GLITCH_ITEM_MATERIAL = Material.CLAY_BALL;
    private final NamespacedKey glitchTypeKey;
    private final GlitchResourcePackRegistry resourcePackRegistry;

    public GlitchItemFactory(GlitchSMP plugin, GlitchResourcePackRegistry resourcePackRegistry) {
        this.glitchTypeKey = new NamespacedKey(plugin, "glitch_type");
        this.resourcePackRegistry = resourcePackRegistry;
    }

    public ItemStack createGlitchItem(GlitchType glitchType) {
        ItemStack item = new ItemStack(GLITCH_ITEM_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + glitchType.getName());
            meta.setCustomModelData(resourcePackRegistry.getModelData(glitchType));
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(glitchTypeKey, PersistentDataType.STRING, glitchType.name());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + glitchType.getDescription());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Right-click to equip this glitch");
            lore.add(ChatColor.YELLOW + "Use /withdraw to remove equipped glitches");
            meta.setLore(lore);

            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isGlitchItem(ItemStack item) {
        return getGlitchType(item).isPresent();
    }

    public Optional<GlitchType> getGlitchType(ItemStack item) {
        if (item == null || item.getType() != GLITCH_ITEM_MATERIAL) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(glitchTypeKey, PersistentDataType.STRING)) {
            String typeName = container.get(glitchTypeKey, PersistentDataType.STRING);
            if (typeName != null) {
                try {
                    return Optional.of(GlitchType.valueOf(typeName));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }

        if (meta.hasCustomModelData()) {
            GlitchType type = resourcePackRegistry.getTypeForModelData(meta.getCustomModelData());
            if (type != null) {
                return Optional.of(type);
            }
            return GlitchType.fromModelData(meta.getCustomModelData());
        }

        return Optional.empty();
    }
}
