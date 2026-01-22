package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, Chunk> activeChunks = new HashMap<>();
    private final Map<UUID, BukkitTask> visualTasks = new HashMap<>();

    public ChunkGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.CHUNK,
            GlitchType.CHUNK.getName(),
            GlitchType.CHUNK.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Chunk chunk = player.getLocation().getChunk();
        UUID uuid = player.getUniqueId();
        activeChunks.put(uuid, chunk);
        effects.playActivation(player, getType());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.6f);

        visualTasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> spawnChunkBorder(chunk), 0L, 20L));
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        activeChunks.remove(uuid);
        BukkitTask task = visualTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        effects.playEnd(player, getType());
        if (activeChunks.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) {
            return;
        }
        for (Map.Entry<UUID, Chunk> entry : activeChunks.entrySet()) {
            Chunk chunk = entry.getValue();
            if (player.getUniqueId().equals(entry.getKey())) {
                continue;
            }
            if (event.getFrom().getChunk().equals(chunk) && !event.getTo().getChunk().equals(chunk)) {
                event.setCancelled(true);
                player.setNoDamageTicks(0);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.5f);
            }
        }
    }

    private void spawnChunkBorder(Chunk chunk) {
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        int centerY = chunk.getWorld().getHighestBlockYAt(minX + 8, minZ + 8) + 1;
        int minY = Math.max(chunk.getWorld().getMinHeight(), centerY - 20);
        int maxY = Math.min(chunk.getWorld().getMaxHeight() - 1, centerY + 20);
        Particle particle = plugin.getGlitchSettings().getChunkBorderParticle();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                chunk.getWorld().spawnParticle(particle, x + 0.5, y, minZ + 0.5, 8, 0.15, 0.15, 0.15, 0.1);
                chunk.getWorld().spawnParticle(particle, x + 0.5, y, maxZ + 0.5, 8, 0.15, 0.15, 0.15, 0.1);
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                chunk.getWorld().spawnParticle(particle, minX + 0.5, y, z + 0.5, 8, 0.15, 0.15, 0.15, 0.1);
                chunk.getWorld().spawnParticle(particle, maxX + 0.5, y, z + 0.5, 8, 0.15, 0.15, 0.15, 0.1);
            }
        }
    }
}
