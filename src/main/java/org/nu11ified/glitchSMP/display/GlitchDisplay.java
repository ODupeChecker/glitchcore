package org.nu11ified.glitchSMP.display;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchResourcePackRegistry;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.manager.GlitchManager;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles displaying equipped glitches above the player's hotbar.
 */
public class GlitchDisplay {
    private final GlitchSMP plugin;
    private final GlitchManager glitchManager;
    private final GlitchResourcePackRegistry resourcePackRegistry;
    
    // Map of player UUIDs to their action bar display tasks
    private final ConcurrentHashMap<UUID, BukkitTask> displayTasks = new ConcurrentHashMap<>();
    
    // Update interval in ticks (1 second = 20 ticks)
    private static final long UPDATE_INTERVAL = 10;
    private static final String EMPTY_SLOT_ICON = "";
    
    /**
     * Constructor for GlitchDisplay
     * 
     * @param plugin The main plugin instance
     * @param glitchManager The glitch manager instance
     */
    public GlitchDisplay(GlitchSMP plugin, GlitchManager glitchManager, GlitchResourcePackRegistry resourcePackRegistry) {
        this.plugin = plugin;
        this.glitchManager = glitchManager;
        this.resourcePackRegistry = resourcePackRegistry;
    }
    
    /**
     * Starts displaying equipped glitches for a player
     * 
     * @param player The player to start displaying for
     */
    public void startDisplaying(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Cancel any existing task
        stopDisplaying(player);
        
        // Start a new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            updateDisplay(player);
        }, 0, UPDATE_INTERVAL);
        
        // Store the task
        displayTasks.put(playerUUID, task);
    }
    
    /**
     * Stops displaying equipped glitches for a player
     * 
     * @param player The player to stop displaying for
     */
    public void stopDisplaying(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Cancel and remove any existing task
        if (displayTasks.containsKey(playerUUID)) {
            displayTasks.get(playerUUID).cancel();
            displayTasks.remove(playerUUID);
        }
    }
    
    /**
     * Updates the display for a player
     * 
     * @param player The player to update the display for
     */
    private void updateDisplay(Player player) {
        Glitch[] slots = glitchManager.getEquippedGlitchSlots(player);
        StringBuilder displayBuilder = new StringBuilder();
        displayBuilder.append(ChatColor.WHITE);
        
        for (int i = 0; i < slots.length; i++) {
            if (i > 0) {
                displayBuilder.append(" ");
            }
            Glitch glitch = slots[i];
            if (glitch == null) {
                displayBuilder.append(EMPTY_SLOT_ICON);
            } else {
                displayBuilder.append(resourcePackRegistry.getIcon(glitch.getType()));
            }
        }
        
        sendActionBar(player, displayBuilder.toString());
    }
    
    /**
     * Sends an action bar message to a player
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    private void sendActionBar(Player player, String message) {
        // Use Paper's API to send action bar message
        player.sendActionBar(message);
    }
    
    /**
     * Starts displaying equipped glitches for all online players
     */
    public void startDisplayingForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            startDisplaying(player);
        }
    }
    
    /**
     * Stops displaying equipped glitches for all online players
     */
    public void stopDisplayingForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopDisplaying(player);
        }
    }
}
