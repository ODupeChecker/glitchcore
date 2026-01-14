package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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

/**
 * Immortality Glitch implementation.
 * Makes the player immune to all damage for the active window.
 */
public class ImmortalityGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> immunePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> auraTasks = new HashMap<>();

    public ImmortalityGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.IMMORTALITY,
            GlitchType.IMMORTALITY.getName(),
            GlitchType.IMMORTALITY.getDescription(),
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
        immunePlayers.add(uuid);
        player.setGlowing(true);
        effects.playActivation(player, getType());
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);

        auraTasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && immunePlayers.contains(uuid)) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.02);
            }
        }, 0L, 10L));
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        immunePlayers.remove(uuid);
        player.setGlowing(false);
        BukkitTask task = auraTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        effects.playEnd(player, getType());
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.9f);
        if (immunePlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (immunePlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.getWorld().strikeLightningEffect(player.getLocation());
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.4f);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
        }
    }
}
