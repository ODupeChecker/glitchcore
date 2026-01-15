package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class HypnosisGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> primedPlayers = new HashSet<>();
    private final Map<UUID, HypnosisSession> sessions = new HashMap<>();
    private final Random random = new Random();

    public HypnosisGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.HYPNOSIS,
            GlitchType.HYPNOSIS.getName(),
            GlitchType.HYPNOSIS.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        primedPlayers.add(player.getUniqueId());
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        primedPlayers.remove(player.getUniqueId());
        effects.playEnd(player, getType());
        if (primedPlayers.isEmpty() && sessions.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!primedPlayers.remove(player.getUniqueId())) {
            return;
        }
        openHypnosisMenu(target);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 0.7f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        HypnosisSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getView().getTitle().equals(session.title())) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != session.inventory()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.GREEN_CONCRETE) {
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.6f);
        int nextRound = session.round() + 1;
        if (nextRound >= 3) {
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            if (primedPlayers.isEmpty() && sessions.isEmpty()) {
                HandlerList.unregisterAll(this);
            }
            return;
        }
        openHypnosisMenu(player, nextRound);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        HypnosisSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getView().getTitle().equals(session.title())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        HypnosisSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getView().getTitle().equals(session.title())) {
            return;
        }
        sessions.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> openHypnosisMenu(player));
    }

    private void openHypnosisMenu(Player player) {
        openHypnosisMenu(player, 0);
    }

    private void openHypnosisMenu(Player player, int round) {
        String title = ChatColor.DARK_GREEN + "CLICK THE GREEN";
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        ItemStack red = createConcrete(Material.RED_CONCRETE);
        ItemStack green = createConcrete(Material.GREEN_CONCRETE);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, red);
        }
        int greenSlot = random.nextInt(inventory.getSize());
        inventory.setItem(greenSlot, green);
        sessions.remove(player.getUniqueId());
        sessions.put(player.getUniqueId(), new HypnosisSession(inventory, round, title));
        player.openInventory(inventory);
    }

    private ItemStack createConcrete(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET.toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private record HypnosisSession(Inventory inventory, int round, String title) {
    }
}
