package org.nu11ified.glitchSMP.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.item.GlitchItemFactory;

/**
 * Command to open the glitches GUI for operators.
 */
public class GlitchesCommand implements CommandExecutor {
    private final GlitchItemFactory glitchItemFactory;

    public GlitchesCommand(GlitchItemFactory glitchItemFactory) {
        this.glitchItemFactory = glitchItemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp() && !player.hasPermission("glitchsmp.command.glitches")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Glitches");
        int slot = 0;
        for (GlitchType glitchType : GlitchType.values()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            ItemStack item = glitchItemFactory.createGlitchItem(glitchType);
            inventory.setItem(slot, item);
            slot++;
        }

        player.openInventory(inventory);
        return true;
    }
}
