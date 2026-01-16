package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class HypnosisGlitch extends Glitch implements Listener {
    private static final String HYPNOSIS_TITLE = "§4Hypnosis Trap";
    private static final int INVENTORY_SIZE = 27;
    private static final int ESCAPE_CLICKS_REQUIRED = 3;
    private static final ItemStack RED_GLASS = createPane(Material.RED_STAINED_GLASS_PANE, "§cEscape?");
    private static final ItemStack GREEN_GLASS = createPane(Material.LIME_STAINED_GLASS_PANE, "§aClick me!");
    private static final Random RANDOM = new Random();

    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, HypnosisSession> sessions = new HashMap<>();

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
        Player target = player.getTargetEntity(12) instanceof Player p ? p : null;
        if (target == null) {
            player.sendMessage("§cNo target found for Hypnosis Glitch.");
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Inventory inventory = Bukkit.createInventory(target, INVENTORY_SIZE, HYPNOSIS_TITLE);
        fillWithRed(inventory);
        int greenSlot = RANDOM.nextInt(INVENTORY_SIZE);
        inventory.setItem(greenSlot, GREEN_GLASS.clone());
        sessions.put(target.getUniqueId(), new HypnosisSession(inventory, greenSlot));
        target.openInventory(inventory);
        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 30, 0.5, 0.6, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 0.7f);
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        effects.playEnd(player, getType());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        HypnosisSession session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory()) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getRawSlot() == session.greenSlot()) {
            int progress = session.incrementProgress();
            if (progress >= ESCAPE_CLICKS_REQUIRED) {
                session.markCompleted();
                sessions.remove(player.getUniqueId());
                player.closeInventory();
                if (sessions.isEmpty()) {
                    HandlerList.unregisterAll(this);
                }
                return;
            }
            refreshGreenSlot(session);
        } else {
            session.resetProgress();
            refreshGreenSlot(session);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        HypnosisSession session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory()) {
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
        if (session == null || session.isCompleted()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(session.inventory()));
    }

    private void refreshGreenSlot(HypnosisSession session) {
        Inventory inventory = session.inventory();
        int nextSlot = RANDOM.nextInt(INVENTORY_SIZE);
        session.setGreenSlot(nextSlot);
        fillWithRed(inventory);
        inventory.setItem(nextSlot, GREEN_GLASS.clone());
    }

    private static void fillWithRed(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, RED_GLASS.clone());
        }
    }

    private static ItemStack createPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class HypnosisSession {
        private final Inventory inventory;
        private int greenSlot;
        private int progress;
        private boolean completed;

        private HypnosisSession(Inventory inventory, int greenSlot) {
            this.inventory = inventory;
            this.greenSlot = greenSlot;
        }

        public Inventory inventory() {
            return inventory;
        }

        public int greenSlot() {
            return greenSlot;
        }

        public void setGreenSlot(int greenSlot) {
            this.greenSlot = greenSlot;
        }

        public int incrementProgress() {
            progress += 1;
            return progress;
        }

        public void resetProgress() {
            progress = 0;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void markCompleted() {
            completed = true;
        }
    }
}
