package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RedstoneGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, Boolean> poweredState = new HashMap<>();
    private final Map<UUID, BukkitTask> scanTasks = new HashMap<>();

    public RedstoneGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.REDSTONE,
            GlitchType.REDSTONE.getName(),
            GlitchType.REDSTONE.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        UUID uuid = player.getUniqueId();
        activePlayers.add(uuid);
        effects.playActivation(player, getType());
        player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.7f, 0.9f);
        scanTasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> scanPower(player), 0L, 10L));
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        activePlayers.remove(uuid);
        poweredState.remove(uuid);
        BukkitTask task = scanTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        effects.playEnd(player, getType());
        if (activePlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (plugin.getAbilityBlocker().isAbilityBlocked(player)) {
            return;
        }
        if (event.getEntity() instanceof Player target && plugin.getAbilityBlocker().isAbilityBlocked(target)) {
            return;
        }
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        if (poweredState.getOrDefault(player.getUniqueId(), false)) {
            event.setDamage(event.getDamage() + 2.0);
            event.getEntity().getWorld().spawnParticle(Particle.DUST, event.getEntity().getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
        }
    }

    private void scanPower(Player player) {
        if (!player.isOnline()) {
            return;
        }
        boolean powered = isNearPoweredRedstone(player.getLocation(), 5);
        poweredState.put(player.getUniqueId(), powered);
        if (powered) {
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
            player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.4f, 1.6f);
        }
    }

    private boolean isNearPoweredRedstone(Location location, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = location.getWorld().getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                    if (block.getType() == Material.REDSTONE_WIRE || block.getType() == Material.REDSTONE_TORCH || block.getType() == Material.REDSTONE_WALL_TORCH) {
                        if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
