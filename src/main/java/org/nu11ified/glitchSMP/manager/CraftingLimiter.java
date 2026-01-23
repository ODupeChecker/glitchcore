package org.nu11ified.glitchSMP.manager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.item.GlitchItemFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Manages crafting limitations and death mechanics for glitches.
 * Ensures players can only have 2 glitches and handles glitch dropping on death.
 */
public class CraftingLimiter implements Listener {
    private final GlitchManager glitchManager;
    private final GlitchItemFactory glitchItemFactory;
    
    // Track how many glitches each player has crafted
    private final Map<UUID, Integer> craftedGlitchCount = new HashMap<>();
    
    // Random generator for selecting which glitch to drop
    private final Random random = new Random();
    
    /**
     * Constructor for CraftingLimiter
     * 
     * @param glitchManager The glitch manager instance
     */
    public CraftingLimiter(GlitchManager glitchManager, GlitchItemFactory glitchItemFactory) {
        this.glitchManager = glitchManager;
        this.glitchItemFactory = glitchItemFactory;
    }
    
    /**
     * Handles crafting events to limit glitch creation
     * 
     * @param event The craft item event
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();
        
        // Check if the crafted item is a glitch
        if (glitchItemFactory.isGlitchItem(result)) {
            // Check if player has reached the glitch limit
            if (hasReachedGlitchLimit(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You can only craft 2 glitches! You must die to lose one before crafting another.");
                return;
            }
            
            // Increment the crafted glitch count
            incrementCraftedGlitchCount(player);
            player.sendMessage(ChatColor.GREEN + "Glitch crafted! You have " + getCraftedGlitchCount(player) + "/2 glitches.");
        }
    }
    
    /**
     * Handles player death to drop one random glitch
     * 
     * @param event The player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<Glitch> equippedGlitches = glitchManager.getEquippedGlitches(player);
        if (equippedGlitches.isEmpty()) {
            return;
        }
        
        // Select a random glitch to drop
        Glitch glitchToDrop = equippedGlitches.get(random.nextInt(equippedGlitches.size()));
        int slot = glitchManager.getEquippedGlitch(player, 0) == glitchToDrop ? 0 : 1;
        
        // Remove the glitch from the player
        glitchManager.unequipGlitch(player, slot);
        
        // Create a glitch item to drop
        ItemStack glitchItem = glitchItemFactory.createGlitchItem(glitchToDrop.getType());
        
        // Drop the glitch item at the death location
        Location deathLocation = player.getLocation();
        player.getWorld().dropItemNaturally(deathLocation, glitchItem);
        
        // Send message to the player
        player.sendMessage(ChatColor.RED + "You dropped " + glitchToDrop.getName() + " on death!");
        
        // Decrement the crafted glitch count
        decrementCraftedGlitchCount(player);
        
        // Send message about being able to craft again
        if (getCraftedGlitchCount(player) < 2) {
            player.sendMessage(ChatColor.YELLOW + "You can now craft " + (2 - getCraftedGlitchCount(player)) + " more glitch(es).");
        }
    }
    
    /**
     * Handles item pickup to prevent duplicate glitches
     * 
     * @param event The player pickup item event
     */
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        
        // Check if the picked up item is a glitch
        if (glitchItemFactory.isGlitchItem(item)) {
            player.sendMessage(ChatColor.GREEN + "You picked up a glitch!");
        }
    }
    
    /**
     * Checks if a player has reached the glitch limit
     * 
     * @param player The player to check
     * @return true if the player has reached the limit, false otherwise
     */
    private boolean hasReachedGlitchLimit(Player player) {
        return getCraftedGlitchCount(player) >= 2;
    }
    
    /**
     * Gets the number of glitches a player has crafted
     * 
     * @param player The player
     * @return The number of crafted glitches
     */
    private int getCraftedGlitchCount(Player player) {
        return craftedGlitchCount.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Increments the crafted glitch count for a player
     * 
     * @param player The player
     */
    private void incrementCraftedGlitchCount(Player player) {
        UUID playerUUID = player.getUniqueId();
        int currentCount = craftedGlitchCount.getOrDefault(playerUUID, 0);
        craftedGlitchCount.put(playerUUID, currentCount + 1);
    }
    
    /**
     * Decrements the crafted glitch count for a player
     * 
     * @param player The player
     */
    private void decrementCraftedGlitchCount(Player player) {
        UUID playerUUID = player.getUniqueId();
        int currentCount = craftedGlitchCount.getOrDefault(playerUUID, 0);
        if (currentCount > 0) {
            craftedGlitchCount.put(playerUUID, currentCount - 1);
        }
    }
    
    /**
     * Checks if an item is a glitch item
     * 
     * @param item The item to check
     * @return true if it's a glitch item, false otherwise
     */
    private boolean isGlitchItem(ItemStack item) {
        return glitchItemFactory.isGlitchItem(item);
    }
    
    /**
     * Cleans up player data when they leave
     * 
     * @param player The player
     */
    public void cleanupPlayerData(Player player) {
        // Note: We don't remove crafted glitch count on disconnect
        // This ensures the limit persists across sessions
    }
    
    /**
     * Resets the crafted glitch count for a player (used internally)
     *
     * @param player The player
     */
    public void resetCraftedGlitchCount(Player player) {
        craftedGlitchCount.remove(player.getUniqueId());
    }
}
