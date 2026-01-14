package org.nu11ified.glitchSMP.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.manager.GlitchManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command executor for the glitch command.
 */
public class GlitchCommand implements CommandExecutor, TabCompleter {
    private final GlitchManager glitchManager;
    
    /**
     * Constructor for GlitchCommand
     * 
     * @param glitchManager The glitch manager instance
     */
    public GlitchCommand(GlitchSMP plugin, GlitchManager glitchManager) {
        this.glitchManager = glitchManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleListCommand(sender, args);
            case "view":
            case "veiw":
                return handleViewCommand(sender, args);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Handles the list subcommand
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleListCommand(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("glitchsmp.command.glitch.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
            // List all available glitch types
            sender.sendMessage(ChatColor.YELLOW + "Available Glitch Types:");
            for (GlitchType type : GlitchType.values()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + type.getName() + ": " + ChatColor.GRAY + type.getDescription());
            }
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            Glitch rightSlot = glitchManager.getEquippedGlitch(player, 0);
            Glitch leftSlot = glitchManager.getEquippedGlitch(player, 1);
            
            sender.sendMessage(ChatColor.YELLOW + "Equipped Glitch Slots:");
            sender.sendMessage(ChatColor.GRAY + "  Right: " + (rightSlot == null ? ChatColor.DARK_GRAY + "Empty" : ChatColor.GREEN + rightSlot.getName()));
            sender.sendMessage(ChatColor.GRAY + "  Left: " + (leftSlot == null ? ChatColor.DARK_GRAY + "Empty" : ChatColor.GREEN + leftSlot.getName()));
            
            sender.sendMessage(ChatColor.YELLOW + "Available Glitch Types:");
            for (GlitchType type : GlitchType.values()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + type.getName() + ": " + ChatColor.GRAY + type.getDescription());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /glitch list [all]");
        }
        
        return true;
    }
    
    /**
     * Handles the view subcommand
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleViewCommand(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /glitch view <player>");
            return true;
        }
        
        Glitch rightSlot = glitchManager.getEquippedGlitch(target, 0);
        Glitch leftSlot = glitchManager.getEquippedGlitch(target, 1);
        
        sender.sendMessage(ChatColor.YELLOW + "=== " + target.getName() + "'s Equipped Glitches ===");
        sender.sendMessage(ChatColor.GRAY + "Right Slot: " + (rightSlot == null ? ChatColor.DARK_GRAY + "Empty" : ChatColor.GREEN + rightSlot.getName()));
        sender.sendMessage(ChatColor.GRAY + "Left Slot: " + (leftSlot == null ? ChatColor.DARK_GRAY + "Empty" : ChatColor.GREEN + leftSlot.getName()));
        return true;
    }
    
    /**
     * Sends the help message to the sender
     * 
     * @param sender The command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Glitch SMP Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/glitch list [all] " + ChatColor.GRAY + "- Lists equipped slots and available glitches");
        sender.sendMessage(ChatColor.YELLOW + "/glitch view [player] " + ChatColor.GRAY + "- View a player's equipped glitches");
        sender.sendMessage(ChatColor.YELLOW + "/glitch help " + ChatColor.GRAY + "- Shows this help message");
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Glitch Activation:");
        sender.sendMessage(ChatColor.GRAY + "• Right-click glitch items to equip them");
        sender.sendMessage(ChatColor.GRAY + "• Use /withdraw to remove equipped glitches");
        sender.sendMessage(ChatColor.GRAY + "• Use offhand keybind (F) to activate right slot glitch");
        sender.sendMessage(ChatColor.GRAY + "• Crouch + offhand keybind to activate left slot glitch");
        sender.sendMessage(ChatColor.GRAY + "• Craft glitches using recipes in recipes.yml");
        sender.sendMessage(ChatColor.GRAY + "• Limited to 2 glitches per player (drops on death)");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest subcommands
            List<String> subcommands = new ArrayList<>();
            subcommands.addAll(Arrays.asList("list", "view", "help"));
            
            return subcommands.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest based on subcommand
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("view")) {
                return org.bukkit.Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (subCommand.equals("list")) {
                // Suggest "all"
                return Arrays.asList("all")
                    .stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}
