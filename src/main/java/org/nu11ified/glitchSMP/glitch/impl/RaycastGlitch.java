package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.DamageTickHelper;

public class RaycastGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final DamageTickHelper damageTickHelper;
    private final GlitchSettings.GlitchProfile profile;

    public RaycastGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.RAYCAST,
            GlitchType.RAYCAST.getName(),
            GlitchType.RAYCAST.getDescription(),
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
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 20, entity -> entity instanceof LivingEntity && entity != player);
        Location start = player.getEyeLocation();
        Location end = result != null && result.getHitPosition() != null ? result.getHitPosition().toLocation(player.getWorld()) : start.clone().add(player.getLocation().getDirection().multiply(20));
        drawBeam(start, end);
        player.getWorld().playSound(start, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);
        effects.playActivation(player, getType());
        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            damageTickHelper.applyDamageTicks(player, target, getType(), profile.baseDamage(), profile.damageTicks(),
                plugin.getGlitchSettings().getCombatDefaults().intervalTicks(),
                plugin.getGlitchSettings().getCombatDefaults().knockbackStrength() * profile.knockbackMultiplier());
        }
    }

    @Override
    protected void onDeactivate(Player player) {
        effects.playEnd(player, getType());
    }

    private void drawBeam(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();
        for (double i = 0; i < length; i += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(i));
            start.getWorld().spawnParticle(Particle.END_ROD, point, 2, 0.02, 0.02, 0.02, 0.01);
        }
        start.getWorld().spawnParticle(Particle.CRIT, end, 12, 0.2, 0.2, 0.2, 0.1);
        start.getWorld().playSound(end, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.9f, 1.6f);
    }
}
