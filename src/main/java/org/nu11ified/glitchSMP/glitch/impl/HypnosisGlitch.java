package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

public class HypnosisGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;

    public HypnosisGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.HYPNOSIS,
            GlitchType.HYPNOSIS.getName(),
            GlitchType.HYPNOSIS.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        Player target = player.getTargetEntity(12) instanceof Player p ? p : null;
        if (target == null) {
            player.sendMessage("§cNo target found for Hypnosis Glitch.");
            return;
        }
        int durationTicks = (int) (getDurationMillis() / 50L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 1, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 2, false, true, true));
        target.getWorld().spawnParticle(Particle.EFFECT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.6, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 0.7f);
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        effects.playEnd(player, getType());
    }
}
