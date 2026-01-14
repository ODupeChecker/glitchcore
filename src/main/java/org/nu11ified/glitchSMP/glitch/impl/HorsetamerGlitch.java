package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorsetamerGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, SkeletonHorse> horses = new HashMap<>();

    public HorsetamerGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.HORSETAMER,
            GlitchType.HORSETAMER.getName(),
            GlitchType.HORSETAMER.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        Location spawnLocation = player.getLocation();
        SkeletonHorse horse = spawnLocation.getWorld().spawn(spawnLocation, SkeletonHorse.class);
        horse.setTamed(true);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.setOwner(player);
        horse.addPassenger(player);
        horses.put(player.getUniqueId(), horse);
        effects.playActivation(player, getType());
        spawnLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLocation, 20, 0.5, 0.3, 0.5, 0.05);
        spawnLocation.getWorld().playSound(spawnLocation, Sound.ENTITY_SKELETON_HORSE_AMBIENT, 1.0f, 1.1f);
    }

    @Override
    protected void onDeactivate(Player player) {
        SkeletonHorse horse = horses.remove(player.getUniqueId());
        if (horse != null && horse.isValid()) {
            horse.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, horse.getLocation(), 10, 0.5, 0.3, 0.5, 0.05);
            horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 0.7f, 1.2f);
            horse.remove();
        }
        effects.playEnd(player, getType());
    }
}
