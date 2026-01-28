package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.DamageTickHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DashGlitch extends Glitch {
    private static final String MODIFIER_NAME = "dash_resist";

    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final DamageTickHelper damageTickHelper;
    private final GlitchSettings.GlitchProfile profile;
    private final Map<UUID, BukkitTask> cleanupTasks = new HashMap<>();

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
        this.damageTickHelper = plugin.getDamageTickHelper();
        this.profile = profile;
    }

    @Override
    protected void onActivate(Player player) {
        Vector velocity = player.getLocation().getDirection().normalize().multiply(2.0);
        player.setVelocity(velocity);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        effects.playActivation(player, getType());

        AttributeModifier modifier = new AttributeModifier(new NamespacedKey(plugin, MODIFIER_NAME), 1.0, AttributeModifier.Operation.ADD_NUMBER);
        if (player.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            player.getAttribute(Attribute.KNOCKBACK_RESISTANCE).addModifier(modifier);
        }

        cleanupTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
                player.getAttribute(Attribute.KNOCKBACK_RESISTANCE).removeModifier(modifier);
            }
        }, 20L));

        if (profile.baseDamage() > 0) {
            Collection<LivingEntity> targets = player.getLocation().getNearbyLivingEntities(2.5, entity -> entity != player);
            for (LivingEntity target : targets) {
                damageTickHelper.applyDamageTicks(player, target, getType(), profile.baseDamage(), profile.damageTicks(),
                    plugin.getGlitchSettings().getCombatDefaults().intervalTicks(),
                    plugin.getGlitchSettings().getCombatDefaults().knockbackStrength() * profile.knockbackMultiplier());
            }
        }
    }

    @Override
    protected void onDeactivate(Player player) {
        BukkitTask task = cleanupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        effects.playEnd(player, getType());
    }
}
