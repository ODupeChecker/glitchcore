package org.nu11ified.glitchSMP.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
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
    private final Set<GlitchType> disabledGlitches = ConcurrentHashMap.newKeySet();
    private final NamespacedKey[] equippedSlotKeys;
    
    /**
     * Constructor for GlitchManager
     * 
     * @param plugin The main plugin instance
     */
    public GlitchManager(GlitchSMP plugin) {
        this.plugin = plugin;
        this.equippedSlotKeys = new NamespacedKey[] {
            new NamespacedKey(plugin, "glitch_slot_0"),
            new NamespacedKey(plugin, "glitch_slot_1")
        };
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

        int duplicateSlot = findSlotWithType(slots, glitch.getType());
        if (duplicateSlot != -1) {
            withdrawSlot(player, duplicateSlot);
        }
        
        for (int i = 0; i < MAX_EQUIPPED_GLITCHES; i++) {
            if (slots[i] == null) {
                slots[i] = glitch;
                persistGlitchSlot(player, i, glitch);
                glitch.onEquip(player);
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
        if (removed != null) {
            removed.onUnequip(player);
        }
        
        slots[slot] = null;
        persistGlitchSlot(player, slot, null);
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
        
        // Check if player has the glitch equipped
        if (!isGlitchEquipped(player, glitch)) {
            return false;
        }
        if (!isGlitchEnabled(glitch.getType())) {
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
     * Checks whether a glitch type is enabled.
     *
     * @param type The glitch type to check
     * @return true if enabled, false if disabled
     */
    public boolean isGlitchEnabled(GlitchType type) {
        return !disabledGlitches.contains(type);
    }

    /**
     * Disable a glitch type and end any active instances for online players.
     *
     * @param type The glitch type to disable
     */
    public void disableGlitch(GlitchType type) {
        disabledGlitches.add(type);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Glitch[] slots = equippedGlitches.get(player.getUniqueId());
            if (slots == null) {
                continue;
            }
            for (Glitch glitch : slots) {
                if (glitch != null && glitch.getType() == type && isGlitchActive(player, glitch)) {
                    deactivateGlitch(player, glitch);
                }
            }
        }
    }

    /**
     * Enable a glitch type.
     *
     * @param type The glitch type to enable
     */
    public void enableGlitch(GlitchType type) {
        disabledGlitches.remove(type);
    }

    /**
     * Gets the set of disabled glitches.
     *
     * @return A set of disabled glitch types
     */
    public Set<GlitchType> getDisabledGlitches() {
        return Collections.unmodifiableSet(disabledGlitches);
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
     * Loads equipped glitches for a player from persistent data storage.
     *
     * @param player The player to load glitches for
     */
    public void loadPlayerData(Player player) {
        Glitch[] slots = new Glitch[MAX_EQUIPPED_GLITCHES];
        PersistentDataContainer container = player.getPersistentDataContainer();

        for (int i = 0; i < MAX_EQUIPPED_GLITCHES; i++) {
            String glitchName = container.get(equippedSlotKeys[i], PersistentDataType.STRING);
            if (glitchName == null || glitchName.isEmpty()) {
                continue;
            }
            try {
                GlitchType glitchType = GlitchType.valueOf(glitchName);
                slots[i] = plugin.getGlitchFactory().createGlitch(glitchType);
            } catch (IllegalArgumentException ignored) {
                container.remove(equippedSlotKeys[i]);
            }
        }

        equippedGlitches.put(player.getUniqueId(), slots);
        for (Glitch glitch : slots) {
            if (glitch != null) {
                glitch.onEquip(player);
            }
        }
        int duplicateSlot = findDuplicateSlot(slots);
        if (duplicateSlot != -1) {
            withdrawSlot(player, duplicateSlot);
        }
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
                if (glitch != null) {
                    glitch.onUnequip(player);
                }
            }
        }
        
        // Remove all task data
        activeGlitchTasks.remove(playerUUID);
        
        // We don't remove equipped glitches here as they should persist
        // between sessions via persistent data storage.
    }

    private void persistGlitchSlot(Player player, int slot, Glitch glitch) {
        if (slot < 0 || slot >= MAX_EQUIPPED_GLITCHES) {
            return;
        }
        PersistentDataContainer container = player.getPersistentDataContainer();
        if (glitch == null) {
            container.remove(equippedSlotKeys[slot]);
        } else {
            container.set(equippedSlotKeys[slot], PersistentDataType.STRING, glitch.getType().name());
        }
    }

    private int findSlotWithType(Glitch[] slots, GlitchType type) {
        if (slots == null) {
            return -1;
        }
        for (int i = 0; i < slots.length; i++) {
            Glitch slotGlitch = slots[i];
            if (slotGlitch != null && slotGlitch.getType() == type) {
                return i;
            }
        }
        return -1;
    }

    private int findDuplicateSlot(Glitch[] slots) {
        if (slots == null || slots.length < 2) {
            return -1;
        }
        Glitch first = slots[0];
        Glitch second = slots[1];
        if (first != null && second != null && first.getType() == second.getType()) {
            return 1;
        }
        return -1;
    }

    private void withdrawSlot(Player player, int slot) {
        Glitch removed = unequipGlitch(player, slot);
        if (removed == null) {
            return;
        }
        ItemStack item = plugin.getGlitchItemFactory().createGlitchItem(removed.getType());
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
        String slotName = slot == 0 ? "right" : "left";
        player.sendMessage("§cDuplicate glitch removed from the " + slotName + " slot and withdrawn.");
    }
}
