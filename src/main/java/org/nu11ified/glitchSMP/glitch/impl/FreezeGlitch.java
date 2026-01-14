package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.DamageTickHelper;

import java.util.List;

public class FreezeGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final DamageTickHelper damageTickHelper;
    private final GlitchSettings.GlitchProfile profile;

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
        Player target = player.getTargetEntity(12) instanceof Player p ? p : null;
        if (target == null) {
            player.sendMessage("§cNo target found for Freeze Glitch.");
            return;
        }
        List<Player> affected = target.getWorld().getPlayers().stream()
            .filter(p -> p.getLocation().distance(target.getLocation()) <= 5)
            .toList();
        for (Player victim : affected) {
            applyFreeze(player, victim);
        }
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        effects.playEnd(player, getType());
    }

    private void applyFreeze(Player source, Player victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (getDurationMillis() / 50L), 5, false, true, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (int) (getDurationMillis() / 50L), 200, false, true, true));
        victim.setFreezeTicks((int) getDurationMillis());
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.1f);
        if (profile.baseDamage() > 0) {
            damageTickHelper.applyDamageTicks(source, victim, getType(), profile.baseDamage(), profile.damageTicks(), plugin.getGlitchSettings().getCombatDefaults().intervalTicks(),
                plugin.getGlitchSettings().getCombatDefaults().knockbackStrength() * profile.knockbackMultiplier());
        }
    }
}
