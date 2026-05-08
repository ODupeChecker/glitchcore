package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
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
import java.util.Map;
import java.util.UUID;

public class GravityGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, BukkitTask> fieldTasks = new HashMap<>();

    public GravityGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.GRAVITY,
            GlitchType.GRAVITY.getName(),
            GlitchType.GRAVITY.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        effects.playActivation(player, getType());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.8f);
        fieldTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskTimer(plugin, () -> applyField(player), 0L, 20L));
    }

    @Override
    protected void onDeactivate(Player player) {
        BukkitTask task = fieldTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        effects.playEnd(player, getType());
    }

    private void applyField(Player owner) {
        if (!owner.isOnline()) {
            return;
        }
        if (plugin.getAbilityBlocker().isAbilityBlocked(owner)) {
            return;
        }
        owner.getWorld().spawnParticle(Particle.INSTANT_EFFECT, owner.getLocation().add(0, 0.5, 0), 12, 1.2, 0.2, 1.2, 0.05);
        for (Player nearby : owner.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(owner.getLocation()) <= 6) {
                if (WorldGuardHook.isBlockedTarget(owner, nearby, plugin.getGlitchSettings().getDisabledRegion(), plugin.getGlitchSettings().getDisabledWorld(), plugin)) {
                    continue;
                }
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 2, false, true, true));
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, true, true));
            }
        }
    }
}
