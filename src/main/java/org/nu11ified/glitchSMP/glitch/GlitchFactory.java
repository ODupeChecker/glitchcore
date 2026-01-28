package org.nu11ified.glitchSMP.glitch;

import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.glitch.impl.BedrockGlitch;
import org.nu11ified.glitchSMP.glitch.impl.ChunkGlitch;
import org.nu11ified.glitchSMP.glitch.impl.DashGlitch;
import org.nu11ified.glitchSMP.glitch.impl.EnchanterGlitch;
import org.nu11ified.glitchSMP.glitch.impl.FakeBlockGlitch;
import org.nu11ified.glitchSMP.glitch.impl.FreezeGlitch;
import org.nu11ified.glitchSMP.glitch.impl.GravityGlitch;
import org.nu11ified.glitchSMP.glitch.impl.HorsetamerGlitch;
import org.nu11ified.glitchSMP.glitch.impl.HypnosisGlitch;
import org.nu11ified.glitchSMP.glitch.impl.ImmortalityGlitch;
import org.nu11ified.glitchSMP.glitch.impl.InventoryGlitch;
import org.nu11ified.glitchSMP.glitch.impl.RaycastGlitch;
import org.nu11ified.glitchSMP.glitch.impl.RedstoneGlitch;
import org.nu11ified.glitchSMP.glitch.impl.RewindGlitch;
import org.nu11ified.glitchSMP.glitch.impl.TelekinesisGlitch;
import org.nu11ified.glitchSMP.glitch.impl.VirusGlitch;
import org.nu11ified.glitchSMP.glitch.impl.WindburstGlitch;

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
        GlitchSettings.GlitchProfile profile = plugin.getGlitchSettings().getProfile(type);
        switch (type) {
            case BEDROCK:
                return new BedrockGlitch(plugin, profile);
            case IMMORTALITY:
                return new ImmortalityGlitch(plugin, profile);
            case INVENTORY:
                return new InventoryGlitch(plugin, profile);
            case REWIND:
                return new RewindGlitch(plugin, profile);
            case CHUNK:
                return new ChunkGlitch(plugin, profile);
            case VIRUS:
                return new VirusGlitch(plugin, profile);
            case ENCHANTER:
                return new EnchanterGlitch(plugin, profile);
            case REDSTONE:
                return new RedstoneGlitch(plugin, profile);
            case FAKE_BLOCK:
                return new FakeBlockGlitch(plugin, profile);
            case FREEZE:
                return new FreezeGlitch(plugin, profile);
            case DASH:
                return new DashGlitch(plugin, profile);
            case WINDBURST:
                return new WindburstGlitch(plugin, profile);
            case HYPNOSIS:
                return new HypnosisGlitch(plugin, profile);
            case GRAVITY:
                return new GravityGlitch(plugin, profile);
            case HORSETAMER:
                return new HorsetamerGlitch(plugin, profile);
            case TELEKINESIS:
                return new TelekinesisGlitch(plugin, profile);
            case RAYCAST:
                return new RaycastGlitch(plugin, profile);
            default:
                return new ImmortalityGlitch(plugin, profile);
        }
    }
}
