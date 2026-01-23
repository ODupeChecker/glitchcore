package org.nu11ified.glitchSMP.manager;

import org.bukkit.entity.Player;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.glitch.Glitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages glitches for all players on the server.
 */
public class GlitchManager {
    private final GlitchSMP plugin;
    
    // Maximum number of glitches a player can equip
    private static final int MAX_EQUIPPED_GLITCHES = 2;
    
    // Map of player UUIDs to their equipped glitches
    private final Map<UUID, Glitch[]> equippedGlitches = new ConcurrentHashMap<>();
    
    // Map of active glitches and their scheduled deactivation tasks
    private final Map<UUID, Map<UUID, Integer>> activeGlitchTasks = new ConcurrentHashMap<>();
    
    /**
     * Constructor for GlitchManager
     * 
     * @param plugin The main plugin instance
     */
    public GlitchManager(GlitchSMP plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Equips a glitch for a player
     * 
     * @param player The player to equip the glitch for
     * @param glitch The glitch to equip
     * @return the slot index that was equipped, or empty if no slot is available
     */
    public OptionalInt equipGlitch(Player player, Glitch glitch) {
        UUID playerUUID = player.getUniqueId();
        Glitch[] slots = equippedGlitches.computeIfAbsent(playerUUID, k -> new Glitch[MAX_EQUIPPED_GLITCHES]);
        
        for (int i = 0; i < MAX_EQUIPPED_GLITCHES; i++) {
            if (slots[i] == null) {
                slots[i] = glitch;
                return OptionalInt.of(i);
            }
        }
        
        return OptionalInt.empty();
    }
    
    /**
     * Unequips a glitch for a player
     * 
     * @param player The player to unequip the glitch for
     * @param slot The slot index to unequip
     * @return The unequipped glitch, or null if none was equipped in that slot
     */
    public Glitch unequipGlitch(Player player, int slot) {
        UUID playerUUID = player.getUniqueId();
        Glitch[] slots = equippedGlitches.get(playerUUID);
        if (slots == null || slot < 0 || slot >= MAX_EQUIPPED_GLITCHES) {
            return null;
        }
        
        Glitch removed = slots[slot];
        if (removed != null && isGlitchActive(player, removed)) {
            deactivateGlitch(player, removed);
        }
        
        slots[slot] = null;
        return removed;
    }
    
    /**
     * Activates a glitch for a player
     * 
     * @param player The player to activate the glitch for
     * @param glitch The glitch to activate
     * @return true if the glitch was activated, false if the player doesn't have the glitch equipped or it's on cooldown
     */
    public boolean activateGlitch(Player player, Glitch glitch) {
        UUID playerUUID = player.getUniqueId();

        if (plugin.getAbilityBlocker().isAbilityBlocked(player)) {
            return false;
        }
        
        // Check if player has the glitch equipped
        if (!isGlitchEquipped(player, glitch)) {
            return false;
        }
        
        // Try to activate the glitch
        if (!glitch.activate(player)) {
            return false;
        }
        
        // If the glitch has a duration, schedule its deactivation
        if (glitch.getDurationMillis() > 0) {
            // Initialize the map for this player if it doesn't exist
            activeGlitchTasks.computeIfAbsent(playerUUID, k -> new HashMap<>());
            
            // Schedule the deactivation task
            int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                plugin,
                () -> deactivateGlitch(player, glitch),
                glitch.getDurationMillis() / 50 // Convert milliseconds to ticks (1 tick = 50ms)
            );
            
            // Store the task ID
            activeGlitchTasks.get(playerUUID).put(glitch.getId(), taskId);
        }
        
        return true;
    }
    
    /**
     * Deactivates a glitch for a player
     * 
     * @param player The player to deactivate the glitch for
     * @param glitch The glitch to deactivate
     */
    public void deactivateGlitch(Player player, Glitch glitch) {
        UUID playerUUID = player.getUniqueId();
        
        // Deactivate the glitch
        glitch.deactivate(player);
        
        // Remove the task if it exists
        if (activeGlitchTasks.containsKey(playerUUID) && activeGlitchTasks.get(playerUUID).containsKey(glitch.getId())) {
            plugin.getServer().getScheduler().cancelTask(activeGlitchTasks.get(playerUUID).get(glitch.getId()));
            activeGlitchTasks.get(playerUUID).remove(glitch.getId());
        }
    }
    
    /**
     * Checks if a player has a glitch equipped
     * 
     * @param player The player to check
     * @param glitch The glitch to check for
     * @return true if the player has the glitch equipped, false otherwise
     */
    public boolean isGlitchEquipped(Player player, Glitch glitch) {
        UUID playerUUID = player.getUniqueId();
        Glitch[] slots = equippedGlitches.get(playerUUID);
        if (slots == null) {
            return false;
        }
        
        for (Glitch slotGlitch : slots) {
            if (slotGlitch == glitch) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a glitch is currently active for a player
     * 
     * @param player The player to check
     * @param glitch The glitch to check
     * @return true if the glitch is active, false otherwise
     */
    public boolean isGlitchActive(Player player, Glitch glitch) {
        UUID playerUUID = player.getUniqueId();
        
        return activeGlitchTasks.containsKey(playerUUID) && 
               activeGlitchTasks.get(playerUUID).containsKey(glitch.getId()) &&
               glitch.isActive();
    }
    
    /**
     * Gets all glitches equipped by a player
     * 
     * @param player The player to get glitches for
     * @return A list of glitches equipped by the player
     */
    public List<Glitch> getEquippedGlitches(Player player) {
        UUID playerUUID = player.getUniqueId();
        Glitch[] slots = equippedGlitches.get(playerUUID);
        if (slots == null) {
            return Collections.emptyList();
        }
        
        List<Glitch> glitches = new ArrayList<>();
        for (Glitch glitch : slots) {
            if (glitch != null) {
                glitches.add(glitch);
            }
        }
        
        return Collections.unmodifiableList(glitches);
    }
    
    /**
     * Gets the equipped glitch in a specific slot
     *
     * @param player The player to get glitches for
     * @param slot The slot index
     * @return The glitch in the slot, or null if empty
     */
    public Glitch getEquippedGlitch(Player player, int slot) {
        UUID playerUUID = player.getUniqueId();
        Glitch[] slots = equippedGlitches.get(playerUUID);
        if (slots == null || slot < 0 || slot >= MAX_EQUIPPED_GLITCHES) {
            return null;
        }
        return slots[slot];
    }
    
    /**
     * Gets a copy of the equipped glitch slots
     *
     * @param player The player to get slots for
     * @return An array of glitches for each slot
     */
    public Glitch[] getEquippedGlitchSlots(Player player) {
        UUID playerUUID = player.getUniqueId();
        Glitch[] slots = equippedGlitches.get(playerUUID);
        if (slots == null) {
            return new Glitch[MAX_EQUIPPED_GLITCHES];
        }
        return slots.clone();
    }
    
    /**
     * Cleans up all glitch data for a player (used when they leave the server)
     * 
     * @param player The player to clean up data for
     */
    public void cleanupPlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Deactivate any active glitches
        Glitch[] slots = equippedGlitches.get(playerUUID);
        if (slots != null) {
            for (Glitch glitch : slots) {
                if (glitch != null && isGlitchActive(player, glitch)) {
                    deactivateGlitch(player, glitch);
                }
            }
        }
        
        // Remove all task data
        activeGlitchTasks.remove(playerUUID);
        
        // We don't remove equipped glitches here as they should persist
        // between sessions. This would be handled by a data storage system.
    }
}
