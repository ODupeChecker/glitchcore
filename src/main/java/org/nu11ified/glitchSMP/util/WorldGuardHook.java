package org.nu11ified.glitchSMP.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Collection;

public final class WorldGuardHook {
    private static final String WORLDGUARD_PLUGIN = "WorldGuard";
    private static boolean warnedMissingWorldGuard;
    private static boolean warnedLookupFailure;

    private WorldGuardHook() {
    }

    public static void resetWarnings() {
        warnedMissingWorldGuard = false;
        warnedLookupFailure = false;
    }

    public static boolean isBlockedTarget(Player source, Player target, String regionId, String worldName, JavaPlugin plugin) {
        if (target == null || !isEnabled(regionId)) {
            return false;
        }
        boolean targetRestricted = isInRegion(target, regionId, worldName, plugin);
        if (!targetRestricted) {
            return false;
        }
        if (source == null) {
            return true;
        }
        return !isInRegion(source, regionId, worldName, plugin);
    }

    public static boolean isInRegion(Player player, String regionId, String worldName, JavaPlugin plugin) {
        if (player == null || !isEnabled(regionId)) {
            return false;
        }
        if (worldName != null && !worldName.isBlank()) {
            if (player.getWorld() == null || !worldName.equalsIgnoreCase(player.getWorld().getName())) {
                return false;
            }
        }
        if (!isWorldGuardAvailable(plugin, regionId)) {
            return false;
        }
        try {
            return isLocationInWorldGuardRegion(player.getLocation(), regionId);
        } catch (ReflectiveOperationException ex) {
            if (!warnedLookupFailure) {
                plugin.getLogger().warning("Failed to query WorldGuard for restricted region '" + regionId + "': " + ex.getMessage());
                warnedLookupFailure = true;
            }
            return false;
        }
    }

    private static boolean isEnabled(String regionId) {
        return regionId != null && !regionId.isBlank();
    }

    private static boolean isWorldGuardAvailable(JavaPlugin plugin, String regionId) {
        Plugin pluginInstance = Bukkit.getPluginManager().getPlugin(WORLDGUARD_PLUGIN);
        if (pluginInstance != null && pluginInstance.isEnabled()) {
            return true;
        }
        if (!warnedMissingWorldGuard) {
            plugin.getLogger().warning("Restricted region is set to '" + regionId + "' but WorldGuard is not installed.");
            warnedMissingWorldGuard = true;
        }
        return false;
    }

    private static boolean isLocationInWorldGuardRegion(Location location, String regionId) throws ReflectiveOperationException {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
        Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
        Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
        Object regionContainer = getRegionContainer.invoke(platform);

        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class)
            .invoke(null, location.getWorld());

        Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
        Method getRegionManager = regionContainer.getClass().getMethod("get", worldClass);
        Object regionManager = getRegionManager.invoke(regionContainer, adaptedWorld);
        if (regionManager == null) {
            return false;
        }

        Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
        Object blockVector = blockVector3Class.getMethod("at", int.class, int.class, int.class)
            .invoke(null, location.getBlockX(), location.getBlockY(), location.getBlockZ());

        Method getApplicableRegions = regionManager.getClass().getMethod("getApplicableRegions", blockVector3Class);
        Object applicableSet = getApplicableRegions.invoke(regionManager, blockVector);
        if (applicableSet == null) {
            return false;
        }

        Method getRegions = applicableSet.getClass().getMethod("getRegions");
        Collection<?> regions = (Collection<?>) getRegions.invoke(applicableSet);
        if (regions == null) {
            return false;
        }

        for (Object region : regions) {
            if (region == null) {
                continue;
            }
            Method getId = region.getClass().getMethod("getId");
            Object id = getId.invoke(region);
            if (id != null && regionId.equalsIgnoreCase(id.toString())) {
                return true;
            }
        }
        return false;
    }
}
