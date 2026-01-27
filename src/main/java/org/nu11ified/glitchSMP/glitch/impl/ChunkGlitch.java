package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
    private final Map<UUID, Integer> activeChunkHeights = new HashMap<>();
    private final Map<UUID, BukkitTask> visualTasks = new HashMap<>();
    private final Particle.DustOptions borderDust = new Particle.DustOptions(Color.fromRGB(255, 32, 32), 1.8f);

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
        activeChunkHeights.put(uuid, player.getLocation().getBlockY());
        effects.playActivation(player, getType());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.6f);

        visualTasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> spawnChunkBorder(chunk, uuid), 0L, 10L));
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        activeChunks.remove(uuid);
        activeChunkHeights.remove(uuid);
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

    private void spawnChunkBorder(Chunk chunk, UUID casterId) {
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        World world = chunk.getWorld();
        int centerY = activeChunkHeights.getOrDefault(casterId, world.getMinHeight());
        int minY = Math.max(world.getMinHeight(), centerY - 10);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + 10);
        int yStep = 2;
        Particle particle = resolveBorderParticle();
        Particle.DustOptions dustOptions = particle.getDataType() == Particle.DustOptions.class ? borderDust : null;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y += yStep) {
                spawnBorderParticle(world, particle, dustOptions, x + 0.5, y + 0.5, minZ + 0.5);
                spawnBorderParticle(world, particle, dustOptions, x + 0.5, y + 0.5, maxZ + 0.5);
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y += yStep) {
                spawnBorderParticle(world, particle, dustOptions, minX + 0.5, y + 0.5, z + 0.5);
                spawnBorderParticle(world, particle, dustOptions, maxX + 0.5, y + 0.5, z + 0.5);
            }
        }
    }

    private Particle resolveBorderParticle() {
        Particle particle = plugin.getGlitchSettings().getChunkBorderParticle();
        if (particle.getDataType() == Void.class || particle.getDataType() == Particle.DustOptions.class) {
            return particle;
        }
        return Particle.DUST;
    }

    private void spawnBorderParticle(World world, Particle particle, Particle.DustOptions dustOptions, double x, double y, double z) {
        world.spawnParticle(particle, x, y, z, 2, 0.0, 0.0, 0.0, 0.0, dustOptions, true);
    }
}
