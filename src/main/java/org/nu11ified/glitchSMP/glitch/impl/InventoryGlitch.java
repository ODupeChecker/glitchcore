package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, InventorySnapshot> snapshots = new HashMap<>();
    private final Map<UUID, UUID> activeTargets = new HashMap<>();
    private final Set<UUID> lockedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> tickTasks = new HashMap<>();

    public InventoryGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.INVENTORY,
            GlitchType.INVENTORY.getName(),
            GlitchType.INVENTORY.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        Player target = getTargetPlayer(player);
        if (target == null) {
            player.sendMessage("§cNo target found for Inventory Glitch.");
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        UUID targetId = target.getUniqueId();
        activeTargets.put(player.getUniqueId(), targetId);
        snapshots.put(targetId, InventorySnapshot.from(target));
        shuffleInventory(target);
        lockedPlayers.add(targetId);
        effects.playActivation(player, getType());
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.5f);
        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);

        tickTasks.put(targetId, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (lockedPlayers.contains(targetId) && target.isOnline()) {
                effects.playTick(target, getType());
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0.02);
            }
        }, 0L, 20L));
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID activatorId = player.getUniqueId();
        UUID targetId = activeTargets.remove(activatorId);
        if (targetId == null) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        InventorySnapshot snapshot = snapshots.remove(targetId);
        if (snapshot != null && target != null && target.isOnline()) {
            snapshot.restore(target);
        }
        lockedPlayers.remove(targetId);
        BukkitTask task = tickTasks.remove(targetId);
        if (task != null) {
            task.cancel();
        }
        if (target != null) {
            effects.playEnd(target, getType());
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        }
        if (lockedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (lockedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (lockedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player getTargetPlayer(Player player) {
        return player.getTargetEntity(10) instanceof Player target ? target : null;
    }

    private void shuffleInventory(Player target) {
        ItemStack[] contents = target.getInventory().getContents();
        List<ItemStack> items = new ArrayList<>();
        Collections.addAll(items, contents);
        Collections.shuffle(items);
        target.getInventory().setContents(items.toArray(new ItemStack[0]));
        target.updateInventory();
    }

    private record InventorySnapshot(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        static InventorySnapshot from(Player player) {
            return new InventorySnapshot(player.getInventory().getContents(), player.getInventory().getArmorContents(), player.getInventory().getItemInOffHand());
        }

        void restore(Player player) {
            player.getInventory().setContents(contents);
            player.getInventory().setArmorContents(armor);
            player.getInventory().setItemInOffHand(offhand);
            player.updateInventory();
        }
    }
}
