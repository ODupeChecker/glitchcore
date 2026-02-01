package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.WorldGuardHook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> primedPlayers = new HashSet<>();
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
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        primedPlayers.add(player.getUniqueId());
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        primedPlayers.remove(player.getUniqueId());
        clearActiveTargetFor(player.getUniqueId(), true);
        if (primedPlayers.isEmpty() && lockedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!primedPlayers.remove(attacker.getUniqueId())) {
            return;
        }
        if (WorldGuardHook.isBlockedTarget(attacker, target, plugin.getGlitchSettings().getDisabledRegion(), plugin.getGlitchSettings().getDisabledWorld(), plugin)) {
            return;
        }
        applyInventoryLock(attacker, target);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!lockedPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getItem() == null) {
            return;
        }
        Material type = event.getItem().getType();
        if (type == Material.WIND_CHARGE || type.isBlock() || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID targetId = event.getEntity().getUniqueId();
        if (!lockedPlayers.contains(targetId)) {
            return;
        }
        removeActiveTargetEntries(targetId);
        clearTarget(targetId, false);
    }

    private void applyInventoryLock(Player source, Player target) {
        UUID targetId = target.getUniqueId();
        activeTargets.put(source.getUniqueId(), targetId);
        lockedPlayers.add(targetId);
        int durationTicks = (int) Math.max(20L, getDurationMillis() / 50L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, durationTicks, 0, false, true, true));
        restrictInventoryUse(target, durationTicks);
        effects.playActivation(target, getType());
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.5f);
        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
    }

    private void restrictInventoryUse(Player target, int durationTicks) {
        UUID targetId = target.getUniqueId();
        tickTasks.put(targetId, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (lockedPlayers.contains(targetId) && target.isOnline()) {
                effects.playTick(target, getType());
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0.02);
            }
        }, 0L, 10L));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> clearTarget(targetId, true), durationTicks);
    }

    private void clearTarget(UUID targetId, boolean playEffects) {
        Player target = Bukkit.getPlayer(targetId);
        lockedPlayers.remove(targetId);
        BukkitTask task = tickTasks.remove(targetId);
        if (task != null) {
            task.cancel();
        }
        if (playEffects && target != null) {
            effects.playEnd(target, getType());
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        }
        if (primedPlayers.isEmpty() && lockedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    private void removeActiveTargetEntries(UUID targetId) {
        activeTargets.entrySet().removeIf(entry -> targetId.equals(entry.getValue()));
    }

    private void clearActiveTargetFor(UUID activatorId, boolean playEffects) {
        UUID targetId = activeTargets.remove(activatorId);
        if (targetId != null) {
            clearTarget(targetId, playEffects);
        }
    }
}
