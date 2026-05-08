package org.nu11ified.glitchSMP.util;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.WorldGuardHook;

public class DamageTickHelper {
    private final Plugin plugin;
    private final GlitchEffects effects;
    private final AbilityBlocker abilityBlocker;

    public DamageTickHelper(Plugin plugin, GlitchEffects effects, AbilityBlocker abilityBlocker) {
        this.plugin = plugin;
        this.effects = effects;
        this.abilityBlocker = abilityBlocker;
    }

    public void applyDamageTicks(Player source, LivingEntity target, GlitchType type, double totalDamage, int ticks, int intervalTicks, double knockbackStrength) {
        if (ticks <= 0) {
            return;
        }
        if (target instanceof Player playerTarget && plugin instanceof org.nu11ified.glitchSMP.GlitchSMP glitchSMP) {
            if (WorldGuardHook.isBlockedTarget(source, playerTarget, glitchSMP.getGlitchSettings().getDisabledRegion(), glitchSMP.getGlitchSettings().getDisabledWorld(), glitchSMP)) {
                return;
            }
        }
        double perTick = totalDamage / ticks;
        Vector knockback = target.getLocation().toVector().subtract(source.getLocation().toVector()).normalize().multiply(knockbackStrength);
        for (int i = 0; i < ticks; i++) {
            int delay = i * intervalTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (target instanceof Player playerTarget && abilityBlocker.isAbilityBlocked(playerTarget)) {
                    return;
                }
                if (!target.isDead()) {
                    target.damage(perTick, source);
                    target.setVelocity(target.getVelocity().add(knockback));
                    target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                    effects.playImpact(target.getLocation(), type);
                }
            }, delay);
        }
    }
}
