package org.nu11ified.glitchSMP.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.item.GlitchItemFactory;
import org.nu11ified.glitchSMP.manager.GlitchManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to withdraw equipped glitches into items.
 */
public class WithdrawCommand implements CommandExecutor, TabCompleter {
    private final GlitchManager glitchManager;
    private final GlitchItemFactory glitchItemFactory;

    public WithdrawCommand(GlitchManager glitchManager, GlitchItemFactory glitchItemFactory) {
        this.glitchManager = glitchManager;
        this.glitchItemFactory = glitchItemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        int slotToWithdraw = -1;

        if (args.length == 0) {
            if (glitchManager.getEquippedGlitch(player, 0) != null) {
                slotToWithdraw = 0;
            } else if (glitchManager.getEquippedGlitch(player, 1) != null) {
                slotToWithdraw = 1;
            }
        } else {
            if ("1".equals(args[0])) {
                slotToWithdraw = 0;
            } else if ("2".equals(args[0])) {
                slotToWithdraw = 1;
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /withdraw [1|2]");
                return true;
            }
        }

        if (slotToWithdraw == -1) {
            player.sendMessage(ChatColor.RED + "You don't have any glitches equipped.");
            return true;
        }

        Glitch equipped = glitchManager.getEquippedGlitch(player, slotToWithdraw);
        if (equipped == null) {
            player.sendMessage(ChatColor.RED + "That slot is already empty.");
            return true;
        }
        if (equipped.isOnCooldown()) {
            long remainingSeconds = Math.max(1L, equipped.getRemainingCooldown() / 1000L);
            player.sendMessage(ChatColor.RED + equipped.getName() + " is on cooldown for " + remainingSeconds + " more seconds!");
            return true;
        }

        Glitch removed = glitchManager.unequipGlitch(player, slotToWithdraw);

        ItemStack item = glitchItemFactory.createGlitchItem(removed.getType());
        Location dropLocation = player.getLocation();
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(dropLocation, item);
        } else {
            player.getInventory().addItem(item);
        }

        String slotName = slotToWithdraw == 0 ? "right" : "left";
        player.sendMessage(ChatColor.GREEN + "Withdrew " + removed.getName() + " from the " + slotName + " slot.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("1", "2")
                .stream()
                .filter(option -> option.startsWith(args[0]))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
