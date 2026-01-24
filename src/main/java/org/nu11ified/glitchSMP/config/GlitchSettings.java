package org.nu11ified.glitchSMP.config;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

public class GlitchSettings {
    private static final String CONFIG_FILE = "glitches.yml";

    private final GlitchSMP plugin;
    private FileConfiguration config;
    private final Map<GlitchType, GlitchProfile> profiles = new EnumMap<>(GlitchType.class);
    private AudioDefaults audioDefaults;
    private VisualDefaults visualDefaults;
    private CombatDefaults combatDefaults;
    private String disabledRegion;
    private int hypnosisEscapeClicks;
    private long telekinesisControlDurationMillis;

    public GlitchSettings(GlitchSMP plugin) {
        this.plugin = plugin;
        reload();
    }

    private void ensureConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File configFile = new File(dataFolder, CONFIG_FILE);
        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE, false);
        }
    }

    private void loadProfiles() {
        ConfigurationSection section = config.getConfigurationSection("perGlitch");
        for (GlitchType type : GlitchType.values()) {
            ConfigurationSection entry = section != null ? section.getConfigurationSection(type.name()) : null;
            long cooldownMillis = toMillis(entry, "cooldownSeconds", type.getCooldownMillis());
            long durationMillis = toMillis(entry, "durationSeconds", type.getDurationMillis());
            double baseDamage = entry != null ? entry.getDouble("baseDamage", 0.0) : 0.0;
            int damageTicks = entry != null ? entry.getInt("damageTicks", combatDefaults.damageTicks()) : combatDefaults.damageTicks();
            double knockbackMultiplier = entry != null ? entry.getDouble("knockbackMultiplier", 1.0) : 1.0;
            profiles.put(type, new GlitchProfile(cooldownMillis, durationMillis, baseDamage, damageTicks, knockbackMultiplier));
        }
    }

    public void reload() {
        ensureConfig();
        this.config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), CONFIG_FILE));
        this.audioDefaults = loadAudioDefaults();
        this.visualDefaults = loadVisualDefaults();
        this.combatDefaults = loadCombatDefaults();
        this.disabledRegion = config.getString("global.protection.disabledRegion", "spawn");
        profiles.clear();
        loadProfiles();
        this.hypnosisEscapeClicks = readEscapeClicks();
        this.telekinesisControlDurationMillis = readTelekinesisControlDurationMillis();
    }

    private long toMillis(ConfigurationSection section, String key, long fallbackMillis) {
        if (section == null || !section.contains(key)) {
            return fallbackMillis;
        }
        return section.getLong(key) * 1000L;
    }

    private AudioDefaults loadAudioDefaults() {
        ConfigurationSection section = config.getConfigurationSection("global.audio");
        return new AudioDefaults(
            readFloat(section, "activation.volume", 1.0f),
            readFloat(section, "activation.pitch", 1.0f),
            readFloat(section, "tick.volume", 0.8f),
            readFloat(section, "tick.pitch", 1.0f),
            readFloat(section, "end.volume", 1.0f),
            readFloat(section, "end.pitch", 0.9f)
        );
    }

    private VisualDefaults loadVisualDefaults() {
        ConfigurationSection section = config.getConfigurationSection("global.visuals");
        int density = section != null ? section.getInt("density", 10) : 10;
        double radius = section != null ? section.getDouble("radius", 1.2) : 1.2;
        int durationTicks = section != null ? section.getInt("durationTicks", 40) : 40;
        return new VisualDefaults(density, radius, durationTicks);
    }

    private CombatDefaults loadCombatDefaults() {
        ConfigurationSection section = config.getConfigurationSection("global.combat");
        int damageTicks = section != null ? section.getInt("damageTicks", 3) : 3;
        int intervalTicks = section != null ? section.getInt("intervalTicks", 5) : 5;
        double knockback = section != null ? section.getDouble("knockbackStrength", 0.4) : 0.4;
        return new CombatDefaults(damageTicks, intervalTicks, knockback);
    }

    private float readFloat(ConfigurationSection section, String key, float fallback) {
        if (section == null || !section.contains(key)) {
            return fallback;
        }
        return (float) section.getDouble(key, fallback);
    }

    public GlitchProfile getProfile(GlitchType type) {
        return profiles.getOrDefault(type, new GlitchProfile(type.getCooldownMillis(), type.getDurationMillis(), 0.0, combatDefaults.damageTicks(), 1.0));
    }

    public AudioDefaults getAudioDefaults() {
        return audioDefaults;
    }

    public VisualDefaults getVisualDefaults() {
        return visualDefaults;
    }

    public Particle getChunkBorderParticle() {
        String particleName = config.getString("perGlitch.CHUNK.borderParticle", Particle.CRIT.name());
        if (particleName == null) {
            return Particle.CRIT;
        }
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            return particle.getDataType() == Void.class ? particle : Particle.CRIT;
        } catch (IllegalArgumentException ex) {
            return Particle.CRIT;
        }
    }

    public CombatDefaults getCombatDefaults() {
        return combatDefaults;
    }

    public String getDisabledRegion() {
        return disabledRegion;
    }

    public int getHypnosisEscapeClicks() {
        return hypnosisEscapeClicks;
    }

    public long getTelekinesisControlDurationMillis() {
        return telekinesisControlDurationMillis;
    }

    public FileConfiguration getRawConfig() {
        return config;
    }

    public record GlitchProfile(long cooldownMillis, long durationMillis, double baseDamage, int damageTicks, double knockbackMultiplier) {
    }

    public record AudioDefaults(float activationVolume, float activationPitch, float tickVolume, float tickPitch, float endVolume, float endPitch) {
    }

    public record VisualDefaults(int density, double radius, int durationTicks) {
    }

    public record CombatDefaults(int damageTicks, int intervalTicks, double knockbackStrength) {
    }

    private int readEscapeClicks() {
        int escapeClicks = config.getInt("perGlitch.HYPNOSIS.escapeClicks", 1);
        return Math.max(1, escapeClicks);
    }

    private long readTelekinesisControlDurationMillis() {
        ConfigurationSection entry = config.getConfigurationSection("perGlitch.TELEKINESIS");
        long fallback = profiles.getOrDefault(GlitchType.TELEKINESIS, new GlitchProfile(
            GlitchType.TELEKINESIS.getCooldownMillis(),
            GlitchType.TELEKINESIS.getDurationMillis(),
            0.0,
            combatDefaults.damageTicks(),
            1.0
        )).durationMillis();
        if (entry == null || !entry.contains("controlDurationSeconds")) {
            return fallback;
        }
        return entry.getLong("controlDurationSeconds") * 1000L;
    }
}
