package org.nu11ified.glitchSMP.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;

public final class WorldGuardHook {
    private static final String WORLDGUARD_PLUGIN = "WorldGuard";

    private WorldGuardHook() {
    }

    public static boolean isBlockedTarget(Player source, Player target, String regionId) {
        if (target == null) {
            return false;
        }
        if (isInRegion(target, regionId)) {
            return true;
        }
        return source != null && isInRegion(source, regionId);
    }

    public static boolean isInRegion(Player player, String regionId) {
        if (player == null || regionId == null) {
            return false;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin(WORLDGUARD_PLUGIN);
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuard);
            Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
            Object regionContainer = getRegionContainer.invoke(platform);

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
            World world = player.getWorld();
            Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, world);
            Class<?> worldEditWorldClass = Class.forName("com.sk89q.worldedit.world.World");
            Object regionManager = regionContainer.getClass().getMethod("get", worldEditWorldClass).invoke(regionContainer, adaptedWorld);
            if (regionManager == null) {
                return false;
            }

            Class<?> blockVectorClass = Class.forName("com.sk89q.worldguard.math.BlockVector3");
            Object blockVector = blockVectorClass.getMethod("at", int.class, int.class, int.class).invoke(
                null,
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
            );

            Object applicableRegions = regionManager.getClass().getMethod("getApplicableRegions", blockVectorClass).invoke(regionManager, blockVector);
            Object regions = applicableRegions.getClass().getMethod("getRegions").invoke(applicableRegions);
            if (regions instanceof Collection<?> collection) {
                for (Object region : collection) {
                    String id = (String) region.getClass().getMethod("getId").invoke(region);
                    if (id != null && id.equalsIgnoreCase(regionId)) {
                        return true;
                    }
                }
            } else if (regions instanceof Iterable<?> iterable) {
                for (Object region : iterable) {
                    String id = (String) region.getClass().getMethod("getId").invoke(region);
                    if (id != null && id.equalsIgnoreCase(regionId)) {
                        return true;
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            return false;
        }
        return false;
    }
}
