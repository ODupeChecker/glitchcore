package org.nu11ified.glitchSMP.glitch;

import org.bukkit.ChatColor;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.glitch.impl.ImmortalityGlitch;

/**
 * Factory class for creating glitch instances.
 */
public class GlitchFactory {
    private final GlitchSMP plugin;
    
    /**
     * Constructor for GlitchFactory
     * 
     * @param plugin The main plugin instance
     */
    public GlitchFactory(GlitchSMP plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Creates a new glitch instance of the specified type
     * 
     * @param type The type of glitch to create
     * @return A new glitch instance
     */
    public Glitch createGlitch(GlitchType type) {
        switch (type) {
            case IMMORTALITY:
                return new ImmortalityGlitch(plugin);
            default:
                // For unimplemented glitches, return a placeholder glitch
                return createPlaceholderGlitch(type);
        }
    }
    
    /**
     * Creates a placeholder glitch for types that haven't been implemented yet
     * 
     * @param type The glitch type
     * @return A placeholder glitch
     */
    private Glitch createPlaceholderGlitch(GlitchType type) {
        return new Glitch(
            type,
            type.getName(),
            type.getDescription(),
            type.getCooldownMillis(),
            type.getDurationMillis()
        ) {
            @Override
            protected void onActivate(org.bukkit.entity.Player player) {
                player.sendMessage(ChatColor.RED + "The " + type.getName() + " is not yet implemented.");
            }
            
            @Override
            protected void onDeactivate(org.bukkit.entity.Player player) {
                // No-op
            }
        };
    }
}
