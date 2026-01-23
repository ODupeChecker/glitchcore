package org.nu11ified.glitchSMP.manager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.item.GlitchItemFactory;
import org.nu11ified.glitchSMP.util.WorldGuardHook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Manages the intuitive activation of glitches through player actions.
 * Handles offhand keybind and crouch detection for activating equipped glitches.
 */
public class ActivationManager implements Listener {
    private final GlitchSMP plugin;
    private final GlitchManager glitchManager;
    private final GlitchItemFactory glitchItemFactory;
    
    // Track which glitch slot each player is currently using
    private final Map<UUID, Integer> currentGlitchSlot = new HashMap<>();
    
    // Track sneaking state for each player
    private final Map<UUID, Boolean> playerSneaking = new HashMap<>();
    
    /**
     * Constructor for ActivationManager
     * 
     * @param plugin The main plugin instance
     * @param glitchManager The glitch manager instance
     */
    public ActivationManager(GlitchSMP plugin, GlitchManager glitchManager, GlitchItemFactory glitchItemFactory) {
        this.plugin = plugin;
        this.glitchManager = glitchManager;
        this.glitchItemFactory = glitchItemFactory;
    }
    
    /**
     * Handles player interaction events (right-clicking with glitch items)
     * 
     * @param event The player interact event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if the player is right-clicking with a glitch item
        if (item != null && glitchItemFactory.isGlitchItem(item)) {
            event.setCancelled(true); // Prevent default item usage
            
            // Get the glitch type from the item
            GlitchType glitchType = glitchItemFactory.getGlitchType(item).orElse(null);
            if (glitchType != null) {
                // Equip the glitch to the first available slot
                Glitch glitch = plugin.getGlitchFactory().createGlitch(glitchType);
                OptionalInt slot = glitchManager.equipGlitch(player, glitch);
                
                if (slot.isPresent()) {
                    int slotIndex = slot.getAsInt();
                    String slotName = slotIndex == 0 ? "right" : "left";
                    player.sendMessage(ChatColor.GREEN + "Equipped " + glitch.getName() + " to the " + slotName + " slot.");
                    
                    // Remove the glitch item from inventory
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().removeItem(item);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Both glitch slots are full. Use /withdraw to free a slot.");
                }
            }
        }
    }
    
    /**
     * Handles offhand keybind events for glitch activation
     * 
     * @param event The player swap hand items event
     */
    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // Cancel the default offhand swap behavior
        event.setCancelled(true);
        
        // Get the player's equipped glitches
        List<Glitch> equippedGlitches = glitchManager.getEquippedGlitches(player);
        
        if (equippedGlitches.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You don't have any glitches equipped!");
            player.sendMessage(ChatColor.YELLOW + "Right-click a glitch item to equip it.");
            return;
        }
        if (WorldGuardHook.isInSpawnRegion(player)) {
            player.sendMessage(ChatColor.RED + "Abilities only work outside of spawn");
            return;
        }
        
        // Determine which glitch to activate based on sneaking state
        boolean isSneaking = playerSneaking.getOrDefault(playerUUID, false);
        int glitchIndex = isSneaking ? 1 : 0; // Left slot (1) if sneaking, right slot (0) if not
        
        // Ensure the glitch index is valid
        Glitch glitchToActivate = glitchManager.getEquippedGlitch(player, glitchIndex);
        if (glitchToActivate == null) {
            player.sendMessage(ChatColor.RED + "No glitch equipped in " + (isSneaking ? "left" : "right") + " slot!");
            return;
        }
        
        // Try to activate the glitch
        boolean success = glitchManager.activateGlitch(player, glitchToActivate);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Activated " + glitchToActivate.getName() + "!");
            
            // Show which slot was used
            String slotName = isSneaking ? "left" : "right";
            player.sendMessage(ChatColor.GRAY + "Used " + slotName + " glitch slot");
            
            // Update the current glitch slot
            currentGlitchSlot.put(playerUUID, glitchIndex);
        } else {
            if (glitchToActivate.isOnCooldown()) {
                long cooldownSeconds = glitchToActivate.getRemainingCooldown() / 1000;
                player.sendMessage(ChatColor.RED + glitchToActivate.getName() + " is on cooldown for " + cooldownSeconds + " more seconds!");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to activate " + glitchToActivate.getName());
            }
        }
    }
    
    /**
     * Handles player sneaking events to track crouch state
     * 
     * @param event The player toggle sneak event
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // Update the sneaking state
        playerSneaking.put(playerUUID, event.isSneaking());
        
        // No slot selection message.
    }
    
    /**
     * Gets the current glitch slot for a player
     * 
     * @param player The player
     * @return The current glitch slot index (0 for right, 1 for left)
     */
    public int getCurrentGlitchSlot(Player player) {
        return currentGlitchSlot.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Checks if a player is currently sneaking
     * 
     * @param player The player
     * @return true if sneaking, false otherwise
     */
    public boolean isPlayerSneaking(Player player) {
        return playerSneaking.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Cleans up player data when they leave
     * 
     * @param player The player
     */
    public void cleanupPlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();
        currentGlitchSlot.remove(playerUUID);
        playerSneaking.remove(playerUUID);
    }
}
