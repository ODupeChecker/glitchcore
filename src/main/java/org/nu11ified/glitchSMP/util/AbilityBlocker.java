package org.nu11ified.glitchSMP.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles checks for ability blocking regions.
 */
public class AbilityBlocker {
    private static final String BLOCKED_WORLD = "worldo";
    private static final String BLOCKED_REGION = "spawn";

    private final boolean worldGuardAvailable;

    public AbilityBlocker(Plugin plugin) {
        this.worldGuardAvailable = plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard");
    }

    public boolean isAbilityBlocked(Player player) {
        if (player == null || !worldGuardAvailable) {
            return false;
        }
        if (!player.getWorld().getName().equalsIgnoreCase(BLOCKED_WORLD)) {
            return false;
        }
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
            .get(BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) {
            return false;
        }
        BlockVector3 position = BlockVector3.at(
            player.getLocation().getBlockX(),
            player.getLocation().getBlockY(),
            player.getLocation().getBlockZ()
        );
        ApplicableRegionSet regions = regionManager.getApplicableRegions(position);
        for (ProtectedRegion region : regions) {
            if (region.getId().equalsIgnoreCase(BLOCKED_REGION)) {
                return true;
            }
        }
        return false;
    }
}
