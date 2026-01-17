package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.DamageTickHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FreezeGlitch extends Glitch implements Listener {
    private static final Particle.DustOptions ICE_DUST = new Particle.DustOptions(Color.fromRGB(90, 170, 255), 1.0f);
    private static final double[][] OUTLINE_POINTS = {
        {-0.45, 0.05, -0.45},
        {-0.45, 0.05, 0.45},
        {0.45, 0.05, -0.45},
        {0.45, 0.05, 0.45},
        {-0.45, 0.9, -0.45},
        {-0.45, 0.9, 0.45},
        {0.45, 0.9, -0.45},
        {0.45, 0.9, 0.45}
    };
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final DamageTickHelper damageTickHelper;
    private final GlitchSettings.GlitchProfile profile;
    private final Set<UUID> primedPlayers = new HashSet<>();

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
        if (primedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!primedPlayers.remove(player.getUniqueId())) {
            return;
        }
        applyFreeze(player, victim);
    }

    private void applyFreeze(Player source, Player victim) {
        int durationTicks = (int) (getDurationMillis() / 50L);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 255, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 200, false, true, true));
        spawnIcePrison(victim, durationTicks);
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.1f);
        if (profile.baseDamage() > 0) {
            damageTickHelper.applyDamageTicks(source, victim, getType(), profile.baseDamage(), profile.damageTicks(), plugin.getGlitchSettings().getCombatDefaults().intervalTicks(),
                plugin.getGlitchSettings().getCombatDefaults().knockbackStrength() * profile.knockbackMultiplier());
        }
    }

    private void spawnIcePrison(Player victim, int durationTicks) {
        BlockDisplay display = victim.getWorld().spawn(victim.getLocation(), BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.ICE.createBlockData());
            spawned.setTransformation(new Transformation(
                new Vector3f(-0.5f, 0f, -0.5f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(1f, 0.55f, 1f),
                new AxisAngle4f(0f, 0f, 0f, 1f)
            ));
            spawned.setPersistent(false);
        });

        BukkitTask particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!victim.isOnline()) {
                return;
            }
            spawnOutlineParticles(victim);
        }, 0L, 5L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            display.remove();
            particleTask.cancel();
        }, durationTicks);
    }

    private void spawnOutlineParticles(Player victim) {
        for (double[] point : OUTLINE_POINTS) {
            victim.getWorld().spawnParticle(
                Particle.REDSTONE,
                victim.getLocation().add(point[0], point[1], point[2]),
                1,
                0,
                0,
                0,
                0,
                ICE_DUST
            );
        }
    }
}
