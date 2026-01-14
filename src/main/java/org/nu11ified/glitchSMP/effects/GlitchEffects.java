package org.nu11ified.glitchSMP.effects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nu11ified.glitchSMP.glitch.GlitchType;

public interface GlitchEffects {
    void playActivation(Player player, GlitchType type);

    void playTick(Player player, GlitchType type);

    void playEnd(Player player, GlitchType type);

    void playImpact(Location location, GlitchType type);
}
