package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakeBlockGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, GhostBlock> ghostBlocks = new HashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();

    public FakeBlockGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.FAKE_BLOCK,
            GlitchType.FAKE_BLOCK.getName(),
            GlitchType.FAKE_BLOCK.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        RayTraceResult result = player.rayTraceBlocks(5);
        if (result == null || result.getHitBlock() == null) {
            player.sendMessage("§cNo block to fake.");
            return;
        }
        Block targetBlock = result.getHitBlock();
        BlockData fakeData = getFakeData(player.getInventory().getItemInMainHand());
        if (fakeData == null) {
            player.sendMessage("§cHold a block to create a fake block.");
            return;
        }
        Location ghostLocation = targetBlock.getRelative(result.getHitBlockFace()).getLocation();
        ghostBlocks.put(player.getUniqueId(), new GhostBlock(ghostLocation, ghostLocation.getBlock().getBlockData(), fakeData));
        sendGhostBlock(player, ghostLocation, fakeData);
        effects.playActivation(player, getType());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.7f, 1.2f);
        player.getWorld().spawnParticle(Particle.CLOUD, ghostLocation.add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.05);

        long durationTicks = Math.max(20L, getDurationMillis() / 50L);
        expiryTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(plugin, () -> clearGhost(player), durationTicks));
    }

    @Override
    protected void onDeactivate(Player player) {
        clearGhost(player);
        if (ghostBlocks.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GhostBlock ghost = ghostBlocks.get(player.getUniqueId());
        if (ghost == null || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getLocation().equals(ghost.location())) {
            clearGhost(player);
        }
    }

    private BlockData getFakeData(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        Material material = item.getType();
        if (!material.isBlock()) {
            return null;
        }
        return material.createBlockData();
    }

    private void sendGhostBlock(Player player, Location location, BlockData data) {
        player.sendBlockChange(location, data);
    }

    private void clearGhost(Player player) {
        UUID uuid = player.getUniqueId();
        GhostBlock ghost = ghostBlocks.remove(uuid);
        if (ghost != null) {
            player.sendBlockChange(ghost.location(), ghost.original());
            player.getWorld().playSound(ghost.location(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.4f);
            player.getWorld().spawnParticle(Particle.SMOKE, ghost.location().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0.02);
        }
        BukkitTask task = expiryTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private record GhostBlock(Location location, BlockData original, BlockData fake) {
    }
}
