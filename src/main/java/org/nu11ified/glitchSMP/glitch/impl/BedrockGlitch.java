package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.Player;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BedrockGlitch extends Glitch implements Listener {
    private static final String METADATA_KEY = "glitch_bedrock";

    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, Map<Location, Material>> placedBlocks = new HashMap<>();
    private final Set<UUID> activePlayers = new HashSet<>();

    public BedrockGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.BEDROCK,
            GlitchType.BEDROCK.getName(),
            GlitchType.BEDROCK.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        activePlayers.add(player.getUniqueId());
        effects.playActivation(player, getType());
        player.playSound(player.getLocation(), Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.8f, 0.7f);
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        activePlayers.remove(uuid);
        Map<Location, Material> blocks = placedBlocks.remove(uuid);
        if (blocks != null) {
            blocks.forEach((location, original) -> {
                Block block = location.getBlock();
                block.removeMetadata(METADATA_KEY, plugin);
                block.setType(original, false);
                Location particleLocation = location.clone().add(0.5, 0.5, 0.5);
                particleLocation.getWorld().spawnParticle(Particle.BLOCK, particleLocation, 12, 0.2, 0.2, 0.2, block.getBlockData());
                particleLocation.getWorld().playSound(particleLocation, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
            });
        }
        effects.playEnd(player, getType());
        if (activePlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        Block block = event.getBlock();
        Material replaced = event.getBlockReplacedState().getType();
        placedBlocks.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).put(block.getLocation().clone(), replaced);
        block.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        block.getWorld().spawnParticle(Particle.ENCHANT, block.getLocation().add(0.5, 1, 0.5), 10, 0.2, 0.4, 0.2, 0.1);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_PLACE, 0.6f, 0.6f);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.hasMetadata(METADATA_KEY)) {
            event.setCancelled(true);
            block.getWorld().spawnParticle(Particle.SMOKE, block.getLocation().add(0.5, 0.8, 0.5), 6, 0.1, 0.1, 0.1, 0.02);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        placedBlocks.values().forEach(map -> map.keySet().removeIf(location -> location.getChunk().equals(event.getChunk())));
    }
}
