package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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

public class WindburstGlitch extends Glitch implements Listener {
    private static final String BARRAGE_KEY = "windburst_barrage";
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final GlitchSettings.WindburstConfig config;
    private final Map<UUID, Integer> hitCounts = new HashMap<>();
    private final Map<UUID, BukkitTask> barrageTasks = new HashMap<>();
    private final NamespacedKey barrageKey;
    private boolean registered;

    public WindburstGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.WINDBURST,
            GlitchType.WINDBURST.getName(),
            GlitchType.WINDBURST.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
        this.config = plugin.getGlitchSettings().getWindburstConfig();
        this.barrageKey = new NamespacedKey(plugin, BARRAGE_KEY);
    }

    @Override
    public void onEquip(Player player) {
        if (!registered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    @Override
    public void onUnequip(Player player) {
        UUID playerId = player.getUniqueId();
        hitCounts.remove(playerId);
        stopBarrage(playerId);
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
    }

    @Override
    protected void onActivate(Player player) {
        startBarrage(player);
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        stopBarrage(player.getUniqueId());
        effects.playEnd(player, getType());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getFinalDamage() <= 0) {
            return;
        }
        if (!plugin.getGlitchManager().isGlitchEquipped(player, this)) {
            return;
        }
        if (!plugin.getGlitchManager().isGlitchEnabled(getType())) {
            return;
        }
        if (WorldGuardHook.isInRegion(player, plugin.getGlitchSettings().getDisabledRegion())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        int hits = hitCounts.getOrDefault(playerId, 0) + 1;
        int threshold = config.passiveHitThreshold();
        if (hits >= threshold) {
            hits -= threshold;
            fireWindCharge(player, false);
        }
        hitCounts.put(playerId, hits);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWindChargeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof WindCharge windCharge)) {
            return;
        }
        if (!isBarrageCharge(windCharge)) {
            return;
        }
        if (windCharge.getShooter() instanceof Player shooter) {
            if (event.getEntity() instanceof Player target
                && WorldGuardHook.isBlockedTarget(shooter, target, plugin.getGlitchSettings().getDisabledRegion(), plugin.getGlitchSettings().getDisabledWorld(), plugin)) {
                return;
            }
        }
        event.setDamage(config.barrageDamage());
    }

    private void startBarrage(Player player) {
        UUID playerId = player.getUniqueId();
        stopBarrage(playerId);
        int barrageCount = config.barrageCount();
        int intervalTicks = config.barrageIntervalTicks();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int fired = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stopBarrage(playerId);
                    return;
                }
                fireWindCharge(player, true);
                fired++;
                if (fired >= barrageCount) {
                    stopBarrage(playerId);
                }
            }
        }, 0L, intervalTicks);
        barrageTasks.put(playerId, task);
    }

    private void stopBarrage(UUID playerId) {
        BukkitTask task = barrageTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void fireWindCharge(Player player, boolean isBarrage) {
        WindCharge windCharge = player.launchProjectile(WindCharge.class);
        if (isBarrage) {
            PersistentDataContainer container = windCharge.getPersistentDataContainer();
            container.set(barrageKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private boolean isBarrageCharge(WindCharge windCharge) {
        return windCharge.getPersistentDataContainer().has(barrageKey, PersistentDataType.BYTE);
    }
}
