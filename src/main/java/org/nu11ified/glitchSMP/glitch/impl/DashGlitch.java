package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.WorldGuardHook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DashGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();
    private final NamespacedKey projectileKey;
    private final NamespacedKey damageKey;

    public DashGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.DASH,
            GlitchType.DASH.getName(),
            GlitchType.DASH.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
        this.projectileKey = new NamespacedKey(plugin, "dash_windcharge");
        this.damageKey = new NamespacedKey(plugin, "dash_windcharge_damage");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    protected void onActivate(Player player) {
        effects.playActivation(player, getType());
        startBarrage(player);
    }

    @Override
    protected void onDeactivate(Player player) {
        effects.playEnd(player, getType());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!plugin.getGlitchManager().isGlitchEquipped(attacker, this)) {
            return;
        }
        if (WorldGuardHook.isBlockedTarget(attacker, target, plugin.getGlitchSettings().getDisabledRegion())) {
            return;
        }
        int threshold = Math.max(1, plugin.getGlitchSettings().getDashPassiveHitThreshold());
        int hits = hitCounters.getOrDefault(attacker.getUniqueId(), 0) + 1;
        if (hits >= threshold) {
            hits = 0;
            fireWindCharge(attacker, plugin.getGlitchSettings().getDashPassiveDamage());
            attacker.sendMessage(ChatColor.WHITE + "Windcharge SHOT");
        }
        hitCounters.put(attacker.getUniqueId(), hits);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof WindCharge windCharge)) {
            return;
        }
        if (!windCharge.getPersistentDataContainer().has(projectileKey, PersistentDataType.INTEGER)) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player target)) {
            return;
        }
        if (!(windCharge.getShooter() instanceof Player shooter)) {
            return;
        }
        if (target.getUniqueId().equals(shooter.getUniqueId())) {
            return;
        }
        if (WorldGuardHook.isBlockedTarget(shooter, target, plugin.getGlitchSettings().getDisabledRegion())) {
            return;
        }
        double damage = windCharge.getPersistentDataContainer().getOrDefault(damageKey, PersistentDataType.DOUBLE, 0.0);
        if (damage <= 0) {
            return;
        }
        target.damage(damage, shooter);
        target.setNoDamageTicks(0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.7f, 1.1f);
    }

    private void startBarrage(Player player) {
        int count = Math.max(1, plugin.getGlitchSettings().getDashBarrageCount());
        int intervalTicks = Math.max(1, plugin.getGlitchSettings().getDashBarrageIntervalTicks());
        double damage = plugin.getGlitchSettings().getDashBarrageDamage();
        int[] remaining = {count};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline() || player.isDead()) {
                task.cancel();
                return;
            }
            fireWindCharge(player, damage);
            remaining[0]--;
            if (remaining[0] <= 0) {
                task.cancel();
            }
        }, 0L, intervalTicks);
    }

    private void fireWindCharge(Player player, double damage) {
        Vector direction = player.getLocation().getDirection().normalize();
        WindCharge windCharge = player.launchProjectile(WindCharge.class);
        windCharge.setVelocity(direction.multiply(1.6));
        windCharge.getPersistentDataContainer().set(projectileKey, PersistentDataType.INTEGER, 1);
        windCharge.getPersistentDataContainer().set(damageKey, PersistentDataType.DOUBLE, damage);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.4f);
    }
}
