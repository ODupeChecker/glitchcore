package org.nu11ified.glitchSMP.effects;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.glitch.GlitchType;

public class DefaultGlitchEffects implements GlitchEffects {
    private final GlitchSettings settings;

    public DefaultGlitchEffects(GlitchSettings settings) {
        this.settings = settings;
    }

    @Override
    public void playActivation(Player player, GlitchType type) {
        GlitchSettings.AudioDefaults audio = settings.getAudioDefaults();
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, audio.activationVolume(), audio.activationPitch());
        spawnAura(player.getLocation(), Particle.END_ROD);
    }

    @Override
    public void playTick(Player player, GlitchType type) {
        GlitchSettings.AudioDefaults audio = settings.getAudioDefaults();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, audio.tickVolume(), audio.tickPitch());
        spawnAura(player.getLocation(), Particle.INSTANT_EFFECT);
    }

    @Override
    public void playEnd(Player player, GlitchType type) {
        GlitchSettings.AudioDefaults audio = settings.getAudioDefaults();
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, audio.endVolume(), audio.endPitch());
        spawnAura(player.getLocation(), Particle.LARGE_SMOKE);
    }

    @Override
    public void playImpact(Location location, GlitchType type) {
        location.getWorld().spawnParticle(Particle.CRIT, location, settings.getVisualDefaults().density(), 0.2, 0.2, 0.2, 0.1);
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
    }

    private void spawnAura(Location location, Particle particle) {
        GlitchSettings.VisualDefaults visuals = settings.getVisualDefaults();
        location.getWorld().spawnParticle(
            particle,
            location,
            visuals.density(),
            visuals.radius(),
            visuals.radius() * 0.6,
            visuals.radius(),
            0.02
        );
    }
}
