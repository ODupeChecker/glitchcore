package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
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

public class TelekinesisGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> primedPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> controlTasks = new HashMap<>();

    public TelekinesisGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.TELEKINESIS,
            GlitchType.TELEKINESIS.getName(),
            GlitchType.TELEKINESIS.getDescription(),
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
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.5f);
    }

    @Override
    protected void onDeactivate(Player player) {
        primedPlayers.remove(player.getUniqueId());
        effects.playEnd(player, getType());
        if (primedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (WorldGuardHook.isBlockedTarget(player, target)) {
            return;
        }
        if (!primedPlayers.remove(player.getUniqueId())) {
            return;
        }
        startTelekinesisControl(player, target);
    }

    private void startTelekinesisControl(Player caster, Player target) {
        int durationTicks = (int) (getDurationMillis() / 50L);
        stopTelekinesisControl(target.getUniqueId());
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, durationTicks, 0, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, 0, false, true, true));
        target.setInvulnerable(false);
        target.setGravity(false);
        target.setVelocity(new Vector(0, 0, 0));
        target.setFallDistance(0f);
        target.getWorld().spawnParticle(Particle.INSTANT_EFFECT, target.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.8f, 1.2f);

        UUID targetId = target.getUniqueId();
        BukkitTask controlTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!caster.isOnline() || !target.isOnline() || target.isDead()) {
                stopTelekinesisControl(targetId);
                return;
            }
            if (!caster.getWorld().equals(target.getWorld())) {
                stopTelekinesisControl(targetId);
                return;
            }
            if (WorldGuardHook.isBlockedTarget(caster, target)) {
                stopTelekinesisControl(targetId);
                return;
            }
            Location casterLocation = caster.getLocation();
            Location eyeLocation = caster.getEyeLocation();
            Vector direction = eyeLocation.getDirection().normalize();
            Location desired = getSafeControlLocation(casterLocation, eyeLocation, direction);
            Location targetLocation = target.getLocation();
            desired.setYaw(targetLocation.getYaw());
            desired.setPitch(targetLocation.getPitch());
            if (!desired.getBlock().isPassable()) {
                return;
            }
            target.teleport(desired);
            target.setInvulnerable(false);
            target.setVelocity(new Vector(0, 0, 0));
            target.setFallDistance(0f);
        }, 0L, 1L);
        controlTasks.put(targetId, controlTask);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> stopTelekinesisControl(targetId), durationTicks);
    }

    private void stopTelekinesisControl(UUID targetId) {
        BukkitTask task = controlTasks.remove(targetId);
        if (task != null) {
            task.cancel();
        }
        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null) {
            target.setGravity(true);
        }
    }

    private Location getSafeControlLocation(Location casterLocation, Location eyeLocation, Vector direction) {
        double distance = 1.0;
        RayTraceResult hit = casterLocation.getWorld().rayTraceBlocks(
            eyeLocation,
            direction,
            distance,
            FluidCollisionMode.NEVER,
            true
        );
        Vector offset = direction.clone().multiply(distance);
        Location desired;
        if (hit != null && hit.getHitPosition() != null) {
            Vector safePosition = hit.getHitPosition().subtract(direction.clone().multiply(0.2));
            desired = safePosition.toLocation(casterLocation.getWorld());
        } else {
            desired = casterLocation.clone().add(offset);
        }
        desired.setY(casterLocation.getY() + 0.5);
        return desired;
    }
}
