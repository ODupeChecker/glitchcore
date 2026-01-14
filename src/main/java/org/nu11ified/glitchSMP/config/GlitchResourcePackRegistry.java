package org.nu11ified.glitchSMP.config;

import org.bukkit.configuration.ConfigurationSection;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class GlitchResourcePackRegistry {
    private final Map<GlitchType, ResourcePackEntry> entries = new EnumMap<>(GlitchType.class);
    private final Map<Integer, GlitchType> modelDataLookup = new HashMap<>();

    public GlitchResourcePackRegistry(GlitchSettings settings) {
        ConfigurationSection section = settings.getRawConfig().getConfigurationSection("resourcePack");
        for (GlitchType type : GlitchType.values()) {
            String icon = type.getActionBarIcon();
            int modelData = type.getModelData();
            if (section != null) {
                ConfigurationSection entry = section.getConfigurationSection(type.name());
                if (entry != null) {
                    icon = entry.getString("icon", icon);
                    modelData = entry.getInt("modelData", modelData);
                }
            }
            entries.put(type, new ResourcePackEntry(icon, modelData));
            modelDataLookup.put(modelData, type);
        }
    }

    public String getIcon(GlitchType type) {
        return entries.get(type).icon();
    }

    public int getModelData(GlitchType type) {
        return entries.get(type).modelData();
    }

    public GlitchType getTypeForModelData(int modelData) {
        return modelDataLookup.get(modelData);
    }

    public record ResourcePackEntry(String icon, int modelData) {
    }
}
