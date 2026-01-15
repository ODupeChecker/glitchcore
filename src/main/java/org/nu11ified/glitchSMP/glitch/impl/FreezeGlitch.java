package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.DamageTickHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FreezeGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final DamageTickHelper damageTickHelper;
    private final GlitchSettings.GlitchProfile profile;
    private final Set<UUID> primedPlayers = new HashSet<>();
    private final Map<UUID, FrozenTarget> frozenTargets = new HashMap<>();

    public FreezeGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.FREEZE,
            GlitchType.FREEZE.getName(),
            GlitchType.FREEZE.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
        this.damageTickHelper = plugin.getDamageTickHelper();
        this.profile = profile;
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
        effects.playEnd(player, getType());
        if (primedPlayers.isEmpty() && frozenTargets.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!primedPlayers.remove(player.getUniqueId())) {
            return;
        }
        applyFreeze(player, target);
        if (primedPlayers.isEmpty() && frozenTargets.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        FrozenTarget frozen = frozenTargets.get(player.getUniqueId());
        if (frozen == null || event.getTo() == null) {
            return;
        }
        if (event.getFrom().getX() == event.getTo().getX()
            && event.getFrom().getY() == event.getTo().getY()
            && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        Location locked = frozen.location();
        event.setTo(locked.clone().setDirection(event.getTo().getDirection()));
    }

    private void applyFreeze(Player source, LivingEntity victim) {
        UUID uuid = victim.getUniqueId();
        if (frozenTargets.containsKey(uuid)) {
            return;
        }
        Location lockLocation = victim.getLocation().clone();
        Block block = lockLocation.getBlock();
        BlockData originalData = block.getBlockData();
        if (block.getType() == Material.AIR) {
            block.setType(Material.BLUE_ICE, false);
        }
        if (victim instanceof Mob mob) {
            mob.setAI(false);
        }
        int durationTicks = (int) (profile.durationMillis() / 50L);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 10, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 250, false, true, true));
        victim.setFreezeTicks((int) profile.durationMillis());
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.1f);
        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> spawnIceBorderParticles(lockLocation), 0L, 4L);
        BukkitTask cleanupTask = Bukkit.getScheduler().runTaskLater(plugin, () -> unfreezeTarget(victim), durationTicks);
        frozenTargets.put(uuid, new FrozenTarget(lockLocation, originalData, particleTask, cleanupTask, victim instanceof Mob));
        if (profile.baseDamage() > 0) {
            damageTickHelper.applyDamageTicks(source, victim, getType(), profile.baseDamage(), profile.damageTicks(), plugin.getGlitchSettings().getCombatDefaults().intervalTicks(),
                plugin.getGlitchSettings().getCombatDefaults().knockbackStrength() * profile.knockbackMultiplier());
        }
    }

    private void unfreezeTarget(LivingEntity victim) {
        FrozenTarget frozen = frozenTargets.remove(victim.getUniqueId());
        if (frozen == null) {
            return;
        }
        frozen.particleTask().cancel();
        frozen.cleanupTask().cancel();
        Block block = frozen.location().getBlock();
        if (block.getType() == Material.BLUE_ICE) {
            block.setBlockData(frozen.originalData(), false);
        }
        if (frozen.restoreAi() && victim instanceof Mob mob) {
            mob.setAI(true);
        }
        if (frozenTargets.isEmpty() && primedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    private void spawnIceBorderParticles(Location location) {
        double baseX = location.getBlockX() + 0.5;
        double baseY = location.getBlockY();
        double baseZ = location.getBlockZ() + 0.5;
        Particle.DustOptions dust = new Particle.DustOptions(Color.AQUA, 1.3f);
        for (double y = baseY; y <= baseY + 1.6; y += 0.4) {
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2) * i / 12;
                double x = baseX + Math.cos(angle) * 0.55;
                double z = baseZ + Math.sin(angle) * 0.55;
                location.getWorld().spawnParticle(Particle.DUST, x, y, z, 2, 0.02, 0.02, 0.02, dust);
            }
        }
    }

    private record FrozenTarget(Location location, BlockData originalData, BukkitTask particleTask, BukkitTask cleanupTask, boolean restoreAi) {
    }
}
