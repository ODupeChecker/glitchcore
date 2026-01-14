package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

public class VirusGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> infected = new HashSet<>();
    private final Map<UUID, BukkitTask> infectionTasks = new HashMap<>();

    public VirusGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.VIRUS,
            GlitchType.VIRUS.getName(),
            GlitchType.VIRUS.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        infect(player);
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        clearInfection(player.getUniqueId());
        if (infected.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!infected.contains(attacker.getUniqueId())) {
            return;
        }
        clearInfection(attacker.getUniqueId());
        infect(target);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_HUSK_AMBIENT, 0.6f, 0.8f);
        target.getWorld().spawnParticle(Particle.ENTITY_EFFECT, target.getLocation().add(0, 1, 0), 14, 0.4, 0.4, 0.4, 0.1);
    }

    private void infect(Player player) {
        UUID uuid = player.getUniqueId();
        infected.add(uuid);
        player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_LOOP, 0.5f, 0.6f);
        effects.playTick(player, getType());
        infectionTasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && infected.contains(uuid)) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.3f, 1.3f);
            }
        }, 0L, 20L));

        long durationTicks = Math.max(20L, getDurationMillis() / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> clearInfection(uuid), durationTicks);
    }

    private void clearInfection(UUID uuid) {
        infected.remove(uuid);
        BukkitTask task = infectionTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
