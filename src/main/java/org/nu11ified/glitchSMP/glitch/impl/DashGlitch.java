package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.WorldGuardHook;

public class DashGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final GlitchSettings.DashConfig config;

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
        this.config = plugin.getGlitchSettings().getDashConfig();
    }

    @Override
    protected void onActivate(Player player) {
        if (WorldGuardHook.isInRegion(player, plugin.getGlitchSettings().getDisabledRegion())) {
            return;
        }
        Vector direction = player.getLocation().getDirection().normalize().multiply(config.dashStrength());
        direction.setY(direction.getY() + config.verticalBoost());
        player.setVelocity(direction);
        effects.playActivation(player, getType());
    }

    @Override
    protected void onDeactivate(Player player) {
        effects.playEnd(player, getType());
    }
}
